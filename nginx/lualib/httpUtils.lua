-- 导入cjson库
local cjson = require('cjson')

cjson.encode_empty_table_as_object(false)

-- 封装函数，发送http请求，并解析响应
local function read_http(path, params)
    ngx.log(ngx.ERR, "http路径：", path)
    local resp = ngx.location.capture(path,{
        method = ngx.HTTP_GET,
        args = params,
    })
    if not resp then
        -- 记录错误信息，返回404
        ngx.log(ngx.ERR, "http查询失败, path: ", path , ", args: ", args)
        ngx.exit(404)
    end
    ngx.log(ngx.ERR, "http返回数据：", resp.body)
    local body = cjson.decode(resp.body)
    return body.data
end

-- 将方法导出
local _M = {  
    read_http = read_http,
}  
return _M