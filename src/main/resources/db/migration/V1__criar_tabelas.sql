CREATE TABLE gasto (
    id             UUID PRIMARY KEY,
    valor_centavos BIGINT       NOT NULL CHECK (valor_centavos > 0),
    descricao      VARCHAR(120) NOT NULL,
    categoria      VARCHAR(20)  NOT NULL,
    motivo         VARCHAR(200),
    quando         TIMESTAMP WITH TIME ZONE NOT NULL,
    autor          VARCHAR(60),
    telefone       VARCHAR(20),
    origem         VARCHAR(10)  NOT NULL,
    wa_message_id  VARCHAR(100) UNIQUE,
    criado_em      TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_gasto_quando ON gasto (quando);
CREATE INDEX idx_gasto_telefone_criado ON gasto (telefone, criado_em);

CREATE TABLE configuracao (
    chave VARCHAR(60)  PRIMARY KEY,
    valor VARCHAR(200) NOT NULL
);
