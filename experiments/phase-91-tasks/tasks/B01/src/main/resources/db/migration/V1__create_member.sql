create table member (
    id bigserial primary key,
    email varchar(255) not null unique,
    created_at timestamp not null default now()
);

insert into member (email) values ('first@example.com'), ('second@example.com');
