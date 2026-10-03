CREATE EXTENSION IF NOT EXISTS unaccent;

-- unaccent() is STABLE; generated columns and indexes need an IMMUTABLE wrapper.
CREATE FUNCTION immutable_unaccent(text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
    AS $$ SELECT public.unaccent('public.unaccent'::regdictionary, $1) $$;

CREATE TABLE genre (
    id   INTEGER PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE title (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tmdb_id         INTEGER      NOT NULL,
    media_type      VARCHAR(10)  NOT NULL CHECK (media_type IN ('MOVIE', 'SERIES')),
    name            VARCHAR(300) NOT NULL,
    original_name   VARCHAR(300),
    release_year    SMALLINT,
    overview        TEXT,
    poster_path     VARCHAR(200),
    backdrop_path   VARCHAR(200),
    runtime_minutes SMALLINT,
    season_count    SMALLINT,
    slug            VARCHAR(320) NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ  NOT NULL,
    search_vector   TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('portuguese', immutable_unaccent(coalesce(name, '') || ' ' || coalesce(original_name, '')))
    ) STORED,
    CONSTRAINT uk_title_tmdb UNIQUE (tmdb_id, media_type)
);

CREATE INDEX idx_title_search ON title USING GIN (search_vector);

CREATE TABLE title_genre (
    title_id BIGINT  NOT NULL REFERENCES title (id) ON DELETE CASCADE,
    genre_id INTEGER NOT NULL REFERENCES genre (id),
    PRIMARY KEY (title_id, genre_id)
);

CREATE TABLE review (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title_id      BIGINT      NOT NULL REFERENCES title (id) ON DELETE CASCADE,
    watched_on    DATE        NOT NULL,
    rating        SMALLINT    NOT NULL CHECK (rating BETWEEN 1 AND 10),
    content       TEXT        NOT NULL DEFAULT '',
    has_spoilers  BOOLEAN     NOT NULL DEFAULT FALSE,
    status        VARCHAR(10) NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED')),
    published_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL,
    search_vector TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('portuguese', immutable_unaccent(content))
    ) STORED
);

CREATE INDEX idx_review_title_watched ON review (title_id, watched_on, created_at);
CREATE INDEX idx_review_published ON review (published_at DESC) WHERE status = 'PUBLISHED';
CREATE INDEX idx_review_search ON review USING GIN (search_vector);
