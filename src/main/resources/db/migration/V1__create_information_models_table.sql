CREATE TABLE information_models (
    id             VARCHAR(255) NOT NULL PRIMARY KEY,
    catalog_id     VARCHAR(50)  NOT NULL,
    published      BOOLEAN      NOT NULL DEFAULT FALSE,
    published_date TIMESTAMP WITH TIME ZONE,
    created        TIMESTAMP WITH TIME ZONE NOT NULL,
    last_modified  TIMESTAMP WITH TIME ZONE,
    uri            VARCHAR(500),
    data           JSONB
);

CREATE INDEX idx_information_models_catalog_id ON information_models (catalog_id);
CREATE INDEX idx_information_models_catalog_published ON information_models (catalog_id, published);
