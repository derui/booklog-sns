# --- !Ups

alter table book change column BOOK_AUTHOR BOOK_AUTHOR VARCHAR(200);
alter table book change column BOOK_ISBN BOOK_ISBN VARCHAR(50);
alter table book change column PUBLISHED_DATE PUBLISHED_DATE DATETIME;
alter table book change column LARGE_IMAGE_URL LARGE_IMAGE_URL VARCHAR(300);
alter table book change column MEDIUM_IMAGE_URL MEDIUM_IMAGE_URL VARCHAR(300);
alter table book change column SMALL_IMAGE_URL SMALL_IMAGE_URL VARCHAR(300);

# --- !Downs
alter table book change column BOOK_AUTHOR BOOK_AUTHOR VARCHAR(200) not null;
alter table book change column BOOK_ISBN BOOK_ISBN VARCHAR(50) not null;
alter table book change column PUBLISHED_DATE PUBLISHED_DATE DATETIME not null;
alter table book change column LARGE_IMAGE_URL LARGE_IMAGE_URL VARCHAR(300) not null;
alter table book change column MEDIUM_IMAGE_URL MEDIUM_IMAGE_URL VARCHAR(300) not null;
alter table book change column SMALL_IMAGE_URL SMALL_IMAGE_URL VARCHAR(300) not null;
