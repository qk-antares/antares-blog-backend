-- /api/ngx/blog/article/{id}，用于获取文章的完整信息
-- 导入httpUtils和redisUtils
local httpUtils = require('httpUtils')
local redisUtils = require('redisUtils')
local read_http = httpUtils.read_http
local read_redis_string = redisUtils.read_redis_string
local increment_redis = redisUtils.increment_redis
local get_set_size = redisUtils.get_set_size
local is_member = redisUtils.is_member
local read_data = redisUtils.read_data

-- 导入cjson库
local cjson = require('cjson')

-- 获取路径参数
local id = ngx.var[1]
-- 获取cookie参数
local token = ngx.var.cookie_TOKEN

-- 查询article信息
local articleCacheKey = "article:id:" .. id .. ":cover"
local articlePath = "/api/blog/article/" .. id .. "/cover"
-- 查询redis
local articleCover = read_redis_string(articleCacheKey)
-- redis查询失败
if not articleCover then
    ngx.log(ngx.ERR, "redis查询失败，尝试查询http， key: ", articleCacheKey)
    -- 去查询http（假定http查询一定有结果）
    articleCover = read_http(articlePath, nil)
-- redis查询成功（还需要拼装浏览数、点赞数、收藏数、评论数、是否点赞和收藏）
else
    local viewCacheKey = "article:id:" .. id .. ":view"
    local likeCacheKey = "article:id:" .. id .. ":like"
    local starCacheKey = "article:id:" .. id .. ":star"
    local commentCacheKey = "article:id:" .. id .. ":comment"
    articleCover = cjson.decode(articleCover)
    -- 将viewCount+1并返回
    local view = increment_redis(viewCacheKey)
    articleCover.viewCount = view
    -- 获取likeCount
    local like = get_set_size(likeCacheKey)
    articleCover.likeCount = like
    -- 获取starCount
    local star = get_set_size(starCacheKey)
    articleCover.starCount = star
    -- 查询commentCount
    local comment = read_redis_string(commentCacheKey)
    articleCover.commentCount = comment
    -- 查询isLiked和isStared
    if token ~= nil then
        local currentUserCacheKey = "user:session:" .. token
        local currentUser = read_redis_string(currentUserCacheKey)
        if not currentUser then
            ngx.log(ngx.ERR, "token非法")
            articleCover.isLiked = false
            articleCover.isStared = false
        else
            currentUser = cjson.decode(currentUser)
            local uid = currentUser.uid
            local isLiked = is_member(likeCacheKey, uid)
            local isStared = is_member(starCacheKey, uid)
            articleCover.isLiked = isLiked
            articleCover.isStared = isStared
        end
    else
        ngx.log(ngx.ERR, "当前用户未登录")
        articleCover.isLiked = false
        articleCover.isStared = false
    end
end

-- 查询content信息
local articleContent = read_data("article:id:" .. id .. ":content", "/api/blog/article/" .. id .. "/content", nil)
articleCover.content = articleContent.content

local response = {
    code = 200,
    msg = '操作成功！',
    data = articleCover
}

-- 把item序列化为json 返回结果
ngx.say(cjson.encode(response))