# scos-foundation-archtest

Módulo de teste puro (`scope=test`, sem código de produção) para regras ArchUnit que citam mais de
um módulo do reactor — ex.: "nada depende de `web`", "nenhum ciclo entre módulos". Nenhum módulo de
implementação enxerga o classpath inteiro sozinho, então essas regras não podem morar dentro de um
módulo individual; `archtest` depende de todos os outros só em escopo de teste para conseguir
verificá-las.

Regras que dizem respeito a um módulo só (ex.: "`core` não importa Spring") **não** entram aqui —
continuam nascendo dentro do próprio módulo protegido, no mesmo commit que o cria (AD-5).

## Regras existentes

Nenhuma ainda. Este módulo nasce vazio de regras — só a estrutura, pronta para receber a primeira
regra cross-módulo (Story 1.14).
