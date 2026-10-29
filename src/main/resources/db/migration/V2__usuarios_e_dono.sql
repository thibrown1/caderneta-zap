-- Cada pessoa tem o próprio login e vê só os próprios gastos.
CREATE TABLE usuario (
    login      VARCHAR(30)  PRIMARY KEY,
    nome       VARCHAR(40)  NOT NULL,
    senha_hash VARCHAR(100) NOT NULL,
    admin      BOOLEAN      NOT NULL,
    criado_em  TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Link de convite: não expira e serve uma vez. Guarda só o hash do código: quem lê o banco não consegue usar o link.
CREATE TABLE convite (
    codigo_hash VARCHAR(64) PRIMARY KEY,
    criado_por  VARCHAR(30) NOT NULL,
    criado_em   TIMESTAMP WITH TIME ZONE NOT NULL,
    expira_em   TIMESTAMP WITH TIME ZONE,
    usado_por   VARCHAR(30),
    usado_em    TIMESTAMP WITH TIME ZONE
);

-- Os gastos que já existem ficam com o dono do painel (PAINEL_USUARIO).
ALTER TABLE gasto ADD COLUMN dono VARCHAR(30);
UPDATE gasto SET dono = LOWER('${dono_padrao}');
ALTER TABLE gasto ALTER COLUMN dono SET NOT NULL;
CREATE INDEX idx_gasto_dono_quando ON gasto (dono, quando);

-- O orçamento passa a ser de cada pessoa.
UPDATE configuracao SET chave = CONCAT('orcamento_mensal_centavos:', LOWER('${dono_padrao}'))
 WHERE chave = 'orcamento_mensal_centavos';
