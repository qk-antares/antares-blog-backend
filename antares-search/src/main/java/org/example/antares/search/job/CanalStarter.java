package org.example.antares.search.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.antares.common.exception.BusinessException;
import org.example.antares.common.model.enums.AppHttpCodeEnum;
import org.example.antares.search.esdao.ArticleEsDao;
import org.example.antares.search.esdao.UserEsDao;
import org.example.antares.search.mapper.ArticleTagMapper;
import org.example.antares.search.mapper.ArticleTagRelationMapper;
import org.example.antares.search.mapper.UserMapper;
import org.example.antares.search.mapper.UserTagMapper;
import org.example.antares.search.model.dto.article.ArticleEsDTO;
import org.example.antares.search.model.dto.user.UserEsDTO;
import org.example.antares.search.model.entity.ArticleTag;
import org.example.antares.search.model.entity.ArticleTagRelation;
import org.example.antares.search.model.entity.User;
import org.example.antares.search.model.entity.UserTag;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

//@Component
@Slf4j
public class CanalStarter implements CommandLineRunner {
    @Resource
    private UserEsDao userEsDao;
    @Resource
    private UserTagMapper userTagMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private ArticleEsDao articleEsDao;
    @Resource
    private ArticleTagRelationMapper articleTagRelationMapper;
    @Resource
    private ArticleTagMapper articleTagMapper;

    @Override
    public void run(String... args) throws Exception {
        // 创建链接
        CanalConnector connector = CanalConnectors.newSingleConnector(
                new InetSocketAddress("1.116.132.238", 11111),
                "example", "", "");
        int batchSize = 1000;
        int emptyCount = 0;
        try {
            connector.connect();
            connector.subscribe("antares_blog.article,antares_blog.user");
            connector.rollback();

            while (true) {
                Message message = connector.getWithoutAck(batchSize); // 获取指定数量的数据
                long batchId = message.getId();
                int size = message.getEntries().size();
                if (batchId == -1 || size == 0) {
                    emptyCount++;
                    log.info("empty count : {}", emptyCount);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                } else {
                    emptyCount = 0;
                    try {
                        processDataChange(message.getEntries());
                    } catch (Exception e) {
                        // 处理失败, 回滚数据
                        connector.rollback(batchId);
                    }
                }
                // 提交确认
                connector.ack(batchId);
            }
        } finally {
            connector.disconnect();
        }
    }

    private void processDataChange(List<CanalEntry.Entry> entries) {
        for (CanalEntry.Entry entry : entries) {
            if (entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONBEGIN || entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONEND) {
                continue;
            }

            CanalEntry.RowChange rowChange;
            try {
                rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            } catch (Exception e) {
                throw new BusinessException(AppHttpCodeEnum.INTERNAL_SERVER_ERROR);
            }

            CanalEntry.EventType eventType = rowChange.getEventType();
            String tableName = entry.getHeader().getTableName();

            log.info("binlog: [{}:{}] , name: [{}.{}] , eventType: {}",
                    entry.getHeader().getLogfileName(), entry.getHeader().getLogfileOffset(),
                    entry.getHeader().getSchemaName(), tableName,
                    eventType);

            switch (tableName){
                case "article": processArticleChange(rowChange, eventType);break;
                case "user": processUserChange(rowChange, eventType);break;
            }
        }
    }

    private void processUserChange(CanalEntry.RowChange rowChange, CanalEntry.EventType eventType) {
        for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
            //用户不存在删除操作
            if (eventType == CanalEntry.EventType.INSERT || eventType == CanalEntry.EventType.UPDATE) {
                UserEsDTO dto = columnsToUserEsDto(rowData.getAfterColumnsList());
                userEsDao.save(dto);
            }
        }
    }

    private void processArticleChange(CanalEntry.RowChange rowChange, CanalEntry.EventType eventType) {
        for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
            //文章不存在删除操作（只有逻辑删除）
            if (eventType == CanalEntry.EventType.INSERT || eventType == CanalEntry.EventType.UPDATE) {
                List<CanalEntry.Column> columnsList = rowData.getAfterColumnsList();
                int isDelete = Integer.parseInt(columnsList.get(21).getValue());
                if (isDelete == 1){
                    articleEsDao.deleteById(Long.parseLong(columnsList.get(0).getValue()));
                } else {
                    ArticleEsDTO dto = columnsToArticleEsDto(columnsList);
                    articleEsDao.save(dto);
                }
            }
        }
    }

    private static void printColumn(List<CanalEntry.Column> columns) {
        for (CanalEntry.Column column : columns) {
            System.out.println(column.getName() + " : " + column.getValue() + "    update=" + column.getUpdated());
        }
    }

    private UserEsDTO columnsToUserEsDto(List<CanalEntry.Column> columns){
        UserEsDTO dto = new UserEsDTO();
        dto.setUid(Long.parseLong(columns.get(0).getValue()));
        dto.setUsername(columns.get(1).getValue());
        dto.setSignature(columns.get(4).getValue());
        List<Long> tagIds = JSON.parseObject(columns.get(3).getValue(), new TypeReference<List<Long>>() {});
        List<String> tags = userTagMapper.selectBatchIds(tagIds).stream().map(UserTag::getName).collect(Collectors.toList());
        dto.setTags(tags);
        return dto;
    }

    private ArticleEsDTO columnsToArticleEsDto(List<CanalEntry.Column> columns){
        ArticleEsDTO dto = new ArticleEsDTO();
        dto.setId(Long.parseLong(columns.get(0).getValue()));
        dto.setTitle(columns.get(1).getValue());
        dto.setSummary(columns.get(2).getValue());
        dto.setContent(columns.get(3).getValue());
        dto.setStatus(Integer.parseInt(columns.get(7).getValue()));
        String username = userMapper.selectOne(new LambdaQueryWrapper<User>().select(User::getUsername)
                .eq(User::getUid, Integer.parseInt(columns.get(16).getValue()))).getUsername();
        dto.setCreatedBy(username);
        dto.setCreateTime(new Date(Long.parseLong(columns.get(19).getValue())));
        dto.setUpdateTime(new Date(Long.parseLong(columns.get(20).getValue())));

        List<Long> tagIds = articleTagRelationMapper.selectList(new LambdaQueryWrapper<ArticleTagRelation>()
                .select(ArticleTagRelation::getTagId)
                .eq(ArticleTagRelation::getArticleId, dto.getId()))
                .stream().map(ArticleTagRelation::getTagId).collect(Collectors.toList());
        List<String> tags = articleTagMapper.selectList(new LambdaQueryWrapper<ArticleTag>()
                .select(ArticleTag::getName)
                .in(ArticleTag::getId, tagIds)).stream().map(ArticleTag::getName).collect(Collectors.toList());
        dto.setTags(tags);
        return dto;
    }
}
