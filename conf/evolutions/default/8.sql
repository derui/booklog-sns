# --- !Ups

alter table book_shelf modify column created_user BIGINT NOT NULL;
alter table book_shelf modify column updated_user BIGINT NOT NULL;

alter table book modify column created_user BIGINT NOT NULL;
alter table book modify column updated_user BIGINT NOT NULL;

alter table RentalInfo rename to rental_info;
alter table rental_info modify column created_user BIGINT NOT NULL;
alter table rental_info modify column updated_user BIGINT NOT NULL;

alter table UserInfo rename to user_info;
alter table user_info modify column created_user BIGINT NOT NULL;
alter table user_info modify column updated_user BIGINT NOT NULL;

# --- !Downs
alter table book_shelf modify column created_user VARCHAR(100) NOT NULL;
alter table book_shelf modify column updated_user VARCHAR(100) NOT NULL;

alter table book modify column created_user VARCHAR(100) NOT NULL;
alter table book modify column updated_user VARCHAR(100) NOT NULL;

alter table rental_info rename to RentalInfo;
alter table RentalInfo modify column created_user VARCHAR(100) NOT NULL;
alter table RentalInfo modify column updated_user VARCHAR(100) NOT NULL;

alter table user_info rename to UserInfo;
alter table UserInfo modify column created_user VARCHAR(100) NOT NULL;
alter table UserInfo modify column updated_user VARCHAR(100) NOT NULL;


