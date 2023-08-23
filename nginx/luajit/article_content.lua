local redisUtils = require('redisUtils')
local read_data = redisUtils.read_data

-- 导入cjson库
local cjson = require('cjson')

-- 获取路径参数
local id = ngx.var[1]

-- 查询content信息
local articleContent = read_data("article:id:" .. id .. ":content", "/api/blog/article/" .. id .. "/content", nil)

local response = {
    code = 200,
    msg = '操作成功！',
    data = articleContent
}

-- 把item序列化为json 返回结果
ngx.say(cjson.encode(response))