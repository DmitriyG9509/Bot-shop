create table if not exists public.paspay_user_names
(
    id                 serial primary key,
    user_name_office   varchar(255)
);

create table if not exists public.product_category
(
    id                 serial primary key,
    category_name      varchar(255)
);

create table if not exists public.product_table
(
    id                 serial primary key,
    price              bigint,
    product_name       varchar(255),
    quantity           integer,
    category_id        bigint
);

create table if not exists public.user_data_table
(
    id                 serial primary key,
    cash               bigint,
    chat_id            bigint,
    duty               bigint,
    name               varchar(255),
    registered_at      timestamp,
    user_name          varchar(255)
);