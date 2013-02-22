# --- !Ups

CREATE TABLE book_shelf
(
	shelf_ID BIGINT AUTO_INCREMENT,
    shelf_name varchar(200) unique,
	PRIMARY KEY(shelf_ID),
    INDEX(shelf_id),
    INDEX(shelf_name)
) DEFAULT CHARSET=utf8 Engine=INNODB;

# --- !Downs
DROP TABLE book_shelf;