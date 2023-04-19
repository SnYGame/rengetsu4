agm.output = {}

local function bufferoutput(...)
    table.insert(agm.output, string.format(...))
end

local function flushoutput()
    local content = table.concat(agm.output, '\n')
    agm.output = {}
    return content
end

agm.bufferoutput = bufferoutput
agm.flushoutput = flushoutput