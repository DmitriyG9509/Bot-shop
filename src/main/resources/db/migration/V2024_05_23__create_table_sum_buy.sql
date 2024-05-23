create table if not exists public.sum_buy
(
    id                 serial primary key,
    name               varchar(255),
    total_sum          bigint,
    chat_id            bigint
    );