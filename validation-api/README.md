# scos-foundation-validation-api

Este artefato não executa nada; a implementação é `scos-foundation-validation`.

Módulo de contrato puro: contém apenas as anotações `@CPF`, `@CNPJ`, `@TaxIdentifier` e `@ZipCode`.
Sem dependência de runtime além de `jakarta.validation-api` — pensado para um módulo de domínio
(ex.: SCOS-Flow) anotar seus campos sem carregar Spring/Hibernate Validator/`caelum-stella`.

Depender só deste módulo compila, mas não valida nada em tempo de execução: sem
`scos-foundation-validation` no classpath, não há `ConstraintValidator` para as anotações abaixo.
Nenhum erro, nenhum aviso — inclua sempre o módulo de implementação junto.

## Fluxo típico de uso

```mermaid
flowchart LR
    App[Aplicação consumidora]

    subgraph api["validation-api (contrato)"]
        Cpf["@CPF"]
        Cnpj["@CNPJ"]
        TaxId["@TaxIdentifier"]
        Zip["@ZipCode"]
    end

    subgraph impl["scos-foundation-validation (implementação)"]
        CpfV["CpfValidator"]
        CnpjV["CnpjValidator"]
        TaxIdV["TaxIdentifierValidator"]
        ZipV["ZipCodeValidator"]
    end

    App -->|anota campo/DTO| Cpf
    App -->|anota campo/DTO| Cnpj
    App -->|anota campo/DTO| TaxId
    App -->|anota campo/DTO| Zip
    Cpf -.só tem efeito com, ligado via META-INF/validation.xml.-> CpfV
    Cnpj -.só tem efeito com, ligado via META-INF/validation.xml.-> CnpjV
    TaxId -.só tem efeito com, ligado via META-INF/validation.xml.-> TaxIdV
    Zip -.só tem efeito com, ligado via META-INF/validation.xml.-> ZipV
```

## Conteúdo

| Tipo | Descrição |
|---|---|
| `@CPF` | Valida um CPF brasileiro (implementação: `CpfValidator`) |
| `@CNPJ` | Valida um CNPJ brasileiro (implementação: `CnpjValidator`) |
| `@TaxIdentifier` | Valida CPF ou CNPJ (implementação: `TaxIdentifierValidator`) |
| `@ZipCode` | Valida um CEP brasileiro (implementação: `ZipCodeValidator`) |

## `validatedBy` vazio, ligado por XML

Cada anotação declara `@Constraint(validatedBy = {})`. O `ConstraintValidator` concreto vive em
`scos-foundation-validation`, que depende deste módulo — referenciá-lo daqui de volta criaria um
ciclo de módulo. A ligação anotação → validador é feita em tempo de execução pelo
`META-INF/validation.xml` (mapeamento XML do Jakarta Bean Validation) empacotado em
`scos-foundation-validation`.

## Regra de fronteira

Uma regra ArchUnit local (`ArchitectureTest`) falha o build se qualquer classe deste módulo não for
`@interface`/`enum`, ou se qualquer dependência além do JDK e `jakarta.validation-api` for
introduzida.
