
drop table if exists product;
CREATE TABLE IF NOT EXISTS product (
  id integer AUTO_INCREMENT,
  version integer,
  available boolean,
  name varchar(255) not null,
   primary key (id)
);


