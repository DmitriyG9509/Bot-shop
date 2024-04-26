create table if not exists public.transactions
(
    id                 serial primary key,
    name               varchar(255),
    price              bigint,
    product_name       varchar(255),
    registered_at      timestamp
);