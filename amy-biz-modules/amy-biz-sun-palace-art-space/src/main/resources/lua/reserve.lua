local stockKey = KEYS[1]

local userKey = KEYS[2]

local reservationId = ARGV[1]

local expireSeconds = ARGV[2]


-- 用户是否已经预约

local oldReservationId =
    redis.call('GET', userKey)

if oldReservationId then

    return 2

end


-- 查询剩余预约资格

local stock =
    redis.call('GET', stockKey)

if not stock then

    return -1

end


stock = tonumber(stock)


-- 没有预约资格

if stock <= 0 then

    return 0

end


-- 扣减资格

redis.call(
    'DECR',
    stockKey
)


-- 写入用户预约记录

redis.call(
    'SET',
    userKey,
    reservationId,
    'EX',
    expireSeconds
)


return 1
