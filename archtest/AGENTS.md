## archtest

## Conventions that differ from defaults

- Um módulo novo do reactor só é coberto pelas regras cross-módulo (`nothingDependsOnWeb`, `noCyclesBetweenModules` em `ArchitectureTest.java`) se for adicionado como dependência de teste em `archtest/pom.xml` — ausência da lista = módulo invisível às regras, não isento delas.

## Known pitfalls

- `README.md` deste módulo diz "Nenhuma [regra] ainda" — desatualizado, há 2 regras hoje (`ArchitectureTest.java`). Não confie no README, leia o teste.
