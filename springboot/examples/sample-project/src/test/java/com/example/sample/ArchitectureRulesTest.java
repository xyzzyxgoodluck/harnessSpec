package com.example.sample;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.Test;

/** 架构测试：把 AGENTS「依赖方向/QueryWrapper 隔离/业务字典只读」等规则固化为可执行断言（随 mvn test 运行）。 */
class ArchitectureRulesTest {

  private static final String MAPPER_PACKAGE = "..mapper..";
  private static final String SERVICE_PACKAGE = "..service..";
  private static final String CONTROLLER_PACKAGE = "..controller..";
  private static final String COMMON_PACKAGE = "..common..";
  private static final String MYBATIS_CONDITIONS = "com.baomidou.mybatisplus.core.conditions..";
  private static final String DICT_MAPPER = "com.example.sample.mapper.DictMapper";
  private static final String DICT_TYPE_MAPPER = "com.example.sample.mapper.DictTypeMapper";
  private static final String DICT_SERVICE_IMPL = "com.example.sample.service.impl.DictServiceImpl";

  private final JavaClasses classes =
      new ClassFileImporter()
          .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
          .importPackages("com.example.sample");

  @Test
  void layersShouldBeFreeOfCycles() {
    ArchRule rule =
        SlicesRuleDefinition.slices()
            .matching("com.example.sample.(*)..")
            .should()
            .beFreeOfCycles();
    rule.check(classes);
  }

  @Test
  void controllerMustNotDependOnMapper() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage(CONTROLLER_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(MAPPER_PACKAGE);
    rule.check(classes);
  }

  @Test
  void mapperMustNotDependOnService() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage(MAPPER_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(SERVICE_PACKAGE);
    rule.check(classes);
  }

  @Test
  void serviceMustNotDependOnController() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage(SERVICE_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(CONTROLLER_PACKAGE);
    rule.check(classes);
  }

  @Test
  void commonMustNotDependOnBusinessLayers() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage(COMMON_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(SERVICE_PACKAGE, CONTROLLER_PACKAGE, MAPPER_PACKAGE, "..entity..");
    rule.check(classes);
  }

  @Test
  void serviceAndControllerMustNotUseMybatisConditionsWrapper() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAnyPackage(SERVICE_PACKAGE, CONTROLLER_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(MYBATIS_CONDITIONS);
    rule.check(classes);
  }

  @Test
  void controllerMustNotDependOnServiceImplementations() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage(CONTROLLER_PACKAGE)
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..service.impl..");
    rule.check(classes);
  }

  @Test
  void onlyDictServiceImplMayDependOnDictMapper() {
    ArchRule rule =
        noClasses()
            .that()
            .doNotHaveFullyQualifiedName(DICT_SERVICE_IMPL)
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName(DICT_MAPPER)
            .because("业务代码禁直写字典表，字典读写只允许经 DictService（CODING_STANDARDS §5）");
    rule.check(classes);
  }

  @Test
  void onlyDictServiceImplMayDependOnDictTypeMapper() {
    ArchRule rule =
        noClasses()
            .that()
            .doNotHaveFullyQualifiedName(DICT_SERVICE_IMPL)
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName(DICT_TYPE_MAPPER)
            .because("字典类型只允许经 DictService 访问（CODING_STANDARDS §5）");
    rule.check(classes);
  }
}
