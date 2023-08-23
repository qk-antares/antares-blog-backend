local redisUtils = require('redisUtils')
local read_data = redisUtils.read_data

-- 导入cjson库
local cjson = require('cjson')

-- 获取路径参数
local id = ngx.var[1]

-- -- 查询cover信息
local articleCacheKey = "article:id:" .. id .. ":cover"
local articlePath = "/api/blog/article/" .. id .. "/basic"
local articleCover = read_data(articleCacheKey, articlePath, nil)

local response = {
    code = 200,
    msg = '操作成功！',
    data = articleCover
}

-- 把item序列化为json 返回结果
ngx.say(cjson.encode(response))