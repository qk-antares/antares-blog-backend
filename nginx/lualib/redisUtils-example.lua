local httpUtils = require('httpUtils')
local read_http = httpUtils.read_http

-- 导入cjson库
local cjson = require('cjson')
-- 导入redis
local redis = require('resty.redis')

cjson.encode_empty_table_as_object(false)

-- 定义ip，port，password
local ip = ""
local port = 6379
local password = ""

-- 关闭redis连接的工具方法，其实是放入连接池
local function close_redis(red)
    local pool_max_idle_time = 60000 -- 连接的空闲时间，单位是毫秒
    local pool_size = 100 --连接池大小
    local ok, err = red:set_keepalive(pool_max_idle_time, pool_size)
    if not ok then
        ngx.log(ngx.ERR, "放入redis连接池失败: ", err)
    end
end

-- redis连接和认证函数
local function redis_init()
    local red = redis:new()
    red:set_timeouts(10000)
    -- 获取一个连接
    local ok, err = red:connect(ip, port)
    if not ok then
        ngx.log(ngx.ERR, "连接redis失败 : ", err)
        return nil
    end

    -- 获取连接重用次数
    local times, err = red:get_reused_times()
    if times == nil or times == 0 then
        -- 认证 Redis
        if password then
            local res, err = red:auth(password)
            if not res then
                ngx.log(ngx.ERR, "Redis 认证失败: ", err)
                return nil
            end
        end
    end
    return red
end

-- 查询redis的方法（string数据结构）
local function read_redis_string(key)
    local red = redis_init()
    -- 获取连接失败
    if red == nil then
        return nil
    end
    -- 查询redis
    local resp, err = red:get(key)
    close_redis(red)
    -- 查询失败处理
    if not resp then
        ngx.log(ngx.ERR, "查询Redis失败: ", err, ", key = " , key)
        return nil
    end
    --得到的数据为空处理
    if resp == ngx.null then
        resp = nil
        ngx.log(ngx.ERR, "查询Redis数据为空, key = ", key)
    end
    return resp
end

-- 查询redis的方法（hash数据结构）
local function read_redis_hash(key)
    local red = redis_init()
    -- 获取连接失败
    if red == nil then
        return nil
    end
    -- 查询redis
    local resp, err = red:hgetall(key)
    close_redis(red)
    -- 查询失败处理
    if not resp then
        ngx.log(ngx.ERR, "查询Redis失败: ", err, ", key = " , key)
        return nil
    end
    --得到的数据为空处理
    if next(resp) == nil then
        ngx.log(ngx.ERR, "查询Redis数据为空, key = ", key)
        return nil
    end
    -- 将field和value组装成lua对象返回
    local data = {}
    for i = 1, #resp, 2 do
        local field = resp[i]
        local value = resp[i + 1]
        data[field] = value
    end
    return data
end

-- 增加count
local function increment_redis(key)
    local red = redis_init()
    -- 获取连接失败
    if red == nil then
        return nil
    end
    -- 执行浏览数+1
    local resp, err = red:incr(key)
    close_redis(red)
    -- 检查Redis命令执行结果
    if not resp then
        ngx.log(ngx.ERR, "failed to execute Redis command: ", err)
        return
    end
    return resp
end

-- 获取set大小（点赞、收藏数）
local function get_set_size(key)
    local red = redis_init()
    -- 获取连接失败
    if red == nil then
        return nil
    end
    local size = red:eval([[
        local size = redis.call('scard', KEYS[1])
        return size
    ]], 1, key)
    close_redis(red)
    return size
end

-- 判断set中是否有成员（是否点赞收藏）
local function is_member(key, member)
    local red = redis_init()
    -- 获取连接失败
    if red == nil then
        return nil
    end
    local res, err = red:sismember(key, member)
    if not res then
        ngx.log(ngx.ERR, "failed to execute SISMEMBER: ", err)
        return false
    end
    close_redis(red)
    return res == 1
end

-- 封装查询函数（适用于string数据结构，查不到了就去请求http）
function read_data(key, path, params)
    -- 查询redis
    local val = read_redis_string(key)
    -- redis查询失败
    if not val then
        ngx.log(ngx.ERR, "redis查询失败，尝试查询http， key: ", key)
        -- redis查询失败，去查询http（假定http查询一定有结果）
        val = read_http(path, params)
    -- redis查询成功
    else
        val = cjson.decode(val);
    end
    -- 返回数据
    return val
end

-- 将方法导出
local _M = {  
    read_redis_string = read_redis_string,
    read_redis_hash = read_redis_hash,
    increment_redis = increment_redis,
    get_set_size = get_set_size,
    is_member = is_member,
    read_data = read_data
}  
return _M