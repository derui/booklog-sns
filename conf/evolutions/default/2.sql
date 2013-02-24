# --- !Ups

CREATE TABLE book
(
	book_ID BIGINT AUTO_INCREMENT,
    shelf_id BIGINT not null,
    book_name varchar(200) not null,
    book_author varchar(200) not null,
    book_isbn varchar(50) not null,
	PRIMARY KEY(book_ID),
    INDEX(shelf_id)
) DEFAULT CHARSET=utf8 Engine=INNODB;

# --- !Downs
DROP TABLE book;
