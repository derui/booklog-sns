# --- !Ups

alter table RentalInfo modify column RENTAL_USER_ID BIGINT NOT NULL;
alter table RentalInfo modify column RENTAL_BOOK_ID BIGINT NOT NULL;
alter table RentalInfo drop index RENTAL_BOOK_ID;
alter table RentalInfo drop index RENTAL_USER_ID;
alter table RentalInfo add unique rentaled_book_index (RENTAL_USER_ID, RENTAL_BOOK_ID);

# --- !Downs
alter table RentalInfo modify column RENTAL_USER_ID BIGINT UNIQUE;
alter table RentalInfo modify column RENTAL_BOOK_ID BIGINT UNIQUE;
alter table RentalInfo drop index rentaled_book_index;
