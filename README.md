# 一文搞懂如何在 Spring Boot 中正确使用 JPA

JPA 这部分内容上手很容易，但是涉及到的东西还是挺多的，网上大部分关于 JPA 的资料都不是特别齐全，大部分用的版本也是比较落后的。我只是把自己觉得比较重要的知识点总结在了下面。我自己也是参考着官方文档写的，[官方文档](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#reference)非常详细了，非常推荐阅读一下。这篇文章可以帮助对 JPA 不了解或者不太熟悉的人来在实际项目中正确使用 JPA。

项目代码基于 Spring Boot 最新的 2.1.9.RELEASE 版本构建（截止到这篇文章写完）

## 1.相关依赖

我们需要下面这些依赖支持我们完成这部分内容的学习：

```xml
 <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
```

## 2.配置数据库连接信息和JPA配置

由于使用的是 h2 内存数据库，所以你直接运行项目数据库就会自动创建好。

下面的配置中需要单独说一下 `spring.jpa.hibernate.ddl-auto=create`这个配置选项。

这个属性常用的选项有四种：

1. `create`:每次重新启动项目都会重新创新表结构，会导致数据丢失
2. `create-drop`:每次启动项目创建表结构，关闭项目删除表结构
3. `update`:每次启动项目会更新表结构
4. `validate`:验证表结构，不对数据库进行任何更改

但是，**一定要不要在生产环境使用 ddl 自动生成表结构，一般推荐手写 SQL 语句配合 Flyway 来做这些事情。**

```properties
# 数据库url地址
spring.datasource.url=jdbc:h2:mem:jpa-demo
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.platform=h2
spring.datasource.driverClassName =org.h2.Driver

spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true
# 打印出 sql 语句
spring.jpa.show-sql=true
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false
server.port=8080
#H2控制台
spring.h2.console.enabled=true

```

## 3.实体类

我们为这个类添加了 `@Entity` 注解代表它是数据库持久化类，还配置了主键 id。

```java
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
@Data
@NoArgsConstructor
public class Person {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String name;
    private Integer age;

    public Person(String name, Integer age) {
        this.name = name;
        this.age = age;
    }

}
```

如何检验你是否正确完成了上面 3 步?很简单，运行项目，查看数据如果发现控制台打印出创建表的 sql 语句，并且数据库中表真的被创建出来的话，说明你成功完成前面 3 步。

控制台打印出来的 sql 语句类似下面这样：

```sql
drop table if exists person
CREATE TABLE `person` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `age` int(11) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL，
   PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
alter table person add constraint UK_p0wr4vfyr2lyifm8avi67mqw5 unique (name)
```

## 4.创建操作数据库的 Repository 接口

```java
@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
}
```

首先这个接口加了 `@Repository` 注解，代表它和数据库操作有关。另外，它继承了 `JpaRepository<Person, Long>`接口，而`JpaRepository<Person, Long>`长这样：

```java

@NoRepositoryBean
public interface JpaRepository<T, ID> extends PagingAndSortingRepository<T, ID>, QueryByExampleExecutor<T> {
    List<T> findAll();

    List<T> findAll(Sort var1);

    List<T> findAllById(Iterable<ID> var1);

    <S extends T> List<S> saveAll(Iterable<S> var1);

    void flush();

    <S extends T> S saveAndFlush(S var1);

    void deleteInBatch(Iterable<T> var1);

    void deleteAllInBatch();

    T getOne(ID var1);

    <S extends T> List<S> findAll(Example<S> var1);

    <S extends T> List<S> findAll(Example<S> var1, Sort var2);
}
```

这表明我们只要继承了` JpaRepository<T, ID>` 就具有了 JPA 为我们提供好的增删改查、分页查询以及根据条件查询等方法。

### 4.1 JPA 自带方法实战

#### 1) 增删改查

**1.保存用户到数据库**

```java
    Person person = new Person("SnailClimb", 23);
    personRepository.save(person);
```

`save()`方法对应 sql 语句就是:`insert into person (age, name) values (23,"snailclimb")`

**2.根据 id 查找用户**

```java
    Optional<Person> personOptional = personRepository.findById(id);
```

`findById()`方法对应 sql 语句就是：`select * from person p where p.id = id`

**3.根据 id 删除用户**

```java
    personRepository.deleteById(id);
```

`deleteById()`方法对应 sql 语句就是：`delete from person where id=id`

**4.更新用户**

更新操作也要通过 `save()`方法来实现，比如：

```java
    Person person = new Person("SnailClimb", 23);
    Person savedPerson = personRepository.save(person);
    // 更新 person 对象的姓名
    savedPerson.setName("UpdatedName");
    personRepository.save(savedPerson);
```

在这里 `save()`方法相当于 sql 语句：`update person set name="UpdatedName" where id=id`

#### 2) 带条件的查询

下面这些方法是我们根据 JPA 提供的语法自定义的，你需要将下面这些方法写到 `PersonRepository` 中。

假如我们想要根据 Name 来查找 Person ，你可以这样：

```java
    Optional<Person> findByName(String name);
```

如果你想要找到年龄大于某个值的人，你可以这样：

```java
    List<Person> findByAgeGreaterThan(int age);
```

### 4.2 自定义 SQL 语句实战

很多时候我们自定义 sql 语句会非常有用。

根据 name 来查找 Person：

```java
    @Query("select p from Person p where p.name = :name")
    Optional<Person> findByNameCustomeQuery(@Param("name") String name);
```

Person 部分属性查询，避免 `select *`操作： 

```java
    @Query("select p.name from Person p where p.id = :id")
    String findPersonNameById(@Param("id") Long id);
```

根据 id 更新Person name：

```java

    @Modifying
    @Query("update Person p set p.name = ?1 where p.id = ?2")
    void updatePersonNameById(String name, Long id);
```

### 4.3 创建异步方法

如果我们需要创建异步方法的话，也比较方便。

异步方法在调用时立即返回，然后会被提交给`TaskExecutor`执行。当然你也可以选择得出结果后才返回给客户端。如果对 Spring Boot 异步编程感兴趣的话可以看这篇文章：[《新手也能看懂的 SpringBoot 异步编程指南》](https://snailclimb.gitee.io/springboot-guide/#/./docs/advanced/springboot-async) 。

```java
@Async
Future<User> findByName(String name);               

@Async
CompletableFuture<User> findByName(String name); 
```

## 5.测试类和源代码地址

测试类：

```java

@SpringBootTest
@RunWith(SpringRunner.class)
public class PersonRepositoryTest {
    @Autowired
    private PersonRepository personRepository;
    private Long id;

    /**
     * 保存person到数据库
     */
    @Before
    public void setUp() {
        assertNotNull(personRepository);
        Person person = new Person("SnailClimb", 23);
        Person savedPerson = personRepository.saveAndFlush(person);// 更新 person 对象的姓名
        savedPerson.setName("UpdatedName");
        personRepository.save(savedPerson);

        id = savedPerson.getId();
    }

    /**
     * 使用 JPA 自带的方法查找 person
     */
    @Test
    public void should_get_person() {
        Optional<Person> personOptional = personRepository.findById(id);
        assertTrue(personOptional.isPresent());
        assertEquals("SnailClimb", personOptional.get().getName());
        assertEquals(Integer.valueOf(23), personOptional.get().getAge());

        List<Person> personList = personRepository.findByAgeGreaterThan(18);
        assertEquals(1, personList.size());
        // 清空数据库
        personRepository.deleteAll();
    }

    /**
     * 自定义 query sql 查询语句查找 person
     */

    @Test
    public void should_get_person_use_custom_query() {
        // 查找所有字段
        Optional<Person> personOptional = personRepository.findByNameCustomeQuery("SnailClimb");
        assertTrue(personOptional.isPresent());
        assertEquals(Integer.valueOf(23), personOptional.get().getAge());
        // 查找部分字段
        String personName = personRepository.findPersonNameById(id);
        assertEquals("SnailClimb", personName);
        System.out.println(id);
        // 更新
        personRepository.updatePersonNameById("UpdatedName", id);
        Optional<Person> updatedName = personRepository.findByNameCustomeQuery("UpdatedName");
        assertTrue(updatedName.isPresent());
        // 清空数据库
        personRepository.deleteAll();
    }

}
```

源代码地址：https://github.com/Snailclimb/springboot-guide/tree/master/source-code/basis/jpa-demo


# JPA 连表查询和分页

对于连表查询，在 JPA 中还是非常常见的，由于  JPA 可以在 respository 层自定义 SQL 语句，所以通过自定义 SQL 语句的方式实现连表还是挺简单。这篇文章是在上一篇[入门 JPA](./springboot-jpa.md)的文章的基础上写的，不了解 JPA 的可以先看上一篇文章。

在[上一节](./springboot-jpa.md)的基础上我们新建了两个实体类，如下：

## 相关实体类创建

`Company.java`

```java
@Entity
@Data
@NoArgsConstructor
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String companyName;
    private String description;

    public Company(String name, String description) {
        this.companyName = name;
        this.description = description;
    }
}
```

`School.java`

```java
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class School {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String name;
    private String description;
}
```

## 自定义 SQL语句实现连表查询

假如我们当前要通过 person 表的 id 来查询 Person 的话，我们知道 Person 的信息一共分布在`Company`、`School`、`Person`这三张表中，所以，我们如果要把 Person 的信息都查询出来的话是需要进行连表查询的。

首先我们需要创建一个包含我们需要的 Person 信息的 DTO 对象,我们简单第将其命名为 `UserDTO`，用于保存和传输我们想要的信息。

```java
@Data
@NoArgsConstructor
@Builder(toBuilder = true)
@AllArgsConstructor
public class UserDTO {
    private String name;
    private int age;
    private String companyName;
    private String schoolName;
}
```

下面我们就来写一个方法查询出 Person 的基本信息。

```java
    /**
     * 连表查询
     */
    @Query(value = "select new github.snailclimb.jpademo.model.dto.UserDTO(p.name,p.age,c.companyName,s.name) " +
            "from Person p left join Company c on  p.companyId=c.id " +
            "left join School s on p.schoolId=s.id " +
            "where p.id=:personId")
    Optional<UserDTO> getUserInformation(@Param("personId") Long personId);
```

可以看出上面的 sql 语句和我们平时写的没啥区别，差别比较大的就是里面有一个 new 对象的操作。

## 自定义 SQL 语句连表查询并实现分页操作

假如我们要查询当前所有的人员信息并实现分页的话，你可以按照下面这种方式来做。可以看到，为了实现分页，我们在`@Query`注解中还添加了 **countQuery** 属性。

```java
@Query(value = "select new github.snailclimb.jpademo.model.dto.UserDTO(p.name,p.age,c.companyName,s.name) " +
        "from Person p left join Company c on  p.companyId=c.id " +
        "left join School s on p.schoolId=s.id ",
        countQuery = "select count(p.id) " +
                "from Person p left join Company c on  p.companyId=c.id " +
                "left join School s on p.schoolId=s.id ")
Page<UserDTO> getUserInformationList(Pageable pageable);
```

实际使用：

```java
//分页选项
PageRequest pageRequest = PageRequest.of(0, 3, Sort.Direction.DESC, "age");
Page<UserDTO> userInformationList = personRepository.getUserInformationList(pageRequest);
//查询结果总数
System.out.println(userInformationList.getTotalElements());// 6
//按照当前分页大小，总页数
System.out.println(userInformationList.getTotalPages());// 2
System.out.println(userInformationList.getContent());
```

## 加餐:自定以SQL语句的其他用法

下面我只介绍两种比较常用的：

1. IN 查询
2. BETWEEN 查询

当然，还有很多用法需要大家自己去实践了。

### IN 查询

 在 sql 语句中加入我们需要筛选出符合几个条件中的一个的情况下，可以使用 IN 查询，对应到 JPA 中也非常简单。比如下面的方法就实现了，根据名字过滤需要的人员信息。

```java
@Query(value = "select new github.snailclimb.jpademo.model.dto.UserDTO(p.name,p.age,c.companyName,s.name) " +
        "from Person p left join Company c on  p.companyId=c.id " +
        "left join School s on p.schoolId=s.id " +
        "where p.name IN :peopleList")
List<UserDTO> filterUserInfo(List peopleList);
```

实际使用:

```java
List<String> personList=new ArrayList<>(Arrays.asList("person1","person2"));
List<UserDTO> userDTOS = personRepository.filterUserInfo(personList);
```

### BETWEEN 查询

查询满足某个范围的值。比如下面的方法就实现查询满足某个年龄范围的人员的信息。

```java
    @Query(value = "select new github.snailclimb.jpademo.model.dto.UserDTO(p.name,p.age,c.companyName,s.name) " +
            "from Person p left join Company c on  p.companyId=c.id " +
            "left join School s on p.schoolId=s.id " +
            "where p.age between :small and :big")
    List<UserDTO> filterUserInfoByAge(int small,int big);
```

实际使用：

```java
List<UserDTO> userDTOS = personRepository.filterUserInfoByAge(19,20);
```


