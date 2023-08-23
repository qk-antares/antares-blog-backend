-- 导入cjson库
local cjson = require('cjson')

local response = {
    code = 200,
    msg = '操作成功！'
}

-- 把item序列化为json 返回结果
ngx.say(cjson.encode(response))