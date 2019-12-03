CREATE TABLE jobs
(
    id            uuid PRIMARY KEY,
    content       text        NOT NULL,
    next_delivery timestamptz NOT NULL
);

CREATE UNIQUE INDEX jobs_pkey ON jobs (id);

--

CREATE TABLE jobs2
(
    id      uuid PRIMARY KEY,
    content text NOT NULL
);

CREATE UNIQUE INDEX jobs2_pkey ON jobs2 (id);

--

CREATE TABLE person
(
    id         uuid PRIMARY KEY,
    first_name text    NOT NULL,
    age        integer NOT NULL
);

CREATE UNIQUE INDEX person_pkey ON person (id);
