
local stack = {buffer = {}}

local StackEntry = {}

function StackEntry.new(name)
    local entry = {name = name}
    setmetatable(entry, {__index = StackEntry})
    return entry
end

function StackEntry:tostring()
    return 'No description set'
end

function StackEntry:execute()
    error('Entry has no effect specified')
end

function stack:buffereffect(entry)
    table.insert(self.buffer, entry)
end

function stack:flushbuffer()
    if #self.buffer == 0 then
        return false
    end
    for _, entry in ipairs(self.buffer) do
        table.insert(self, entry)
    end
    self.buffer = {}
    return true
end

function stack:pop()
    return table.remove(self)
end

function stack:top()
    return self[#self]
end

agm.stack = stack
agm.StackEntry = StackEntry
