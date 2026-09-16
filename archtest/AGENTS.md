## archtest

## Conventions that differ from defaults

- Um módulo novo do reactor só é coberto pelas regras cross-módulo (`nothingDependsOnWeb`, `noCyclesBetweenModules` em `ArchitectureTest.java`) se for adicionado como dependência de teste em `archtest/pom.xml` — ausência da lista = módulo invisível às regras, não isento delas.

## Known pitfalls

- Ao adicionar uma regra cross-módulo nova em `ArchitectureTest.java`, atualize a seção "Regras existentes" do `README.md` deste módulo no mesmo commit — nada mecânico mantém os dois sincronizados. O mesmo vale para a seção "Gate de Javadoc": se o `maven-checkstyle-plugin` do perfil `analyze` (pom.xml raiz) ganhar `includeTestSourceDirectory`/`testSourceDirectories`, a isenção deixa de valer e o README precisa ser atualizado junto.
