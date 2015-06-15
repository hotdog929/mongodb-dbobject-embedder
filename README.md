# mongodb-dbobject-embedder
>使用簡易的語法進行 mongodb 的資料查詢及再處理

## 語法說明
>執行 db.user.find({ height : { $gte : 210 } })會得到以下的結果

```
[
  { "_id" : ObjectId("557e58727a8ea2a9dfe2ef76"), "name" : "Kirk", "postal_code" : ObjectId("557e56287a8ea2a9dfe2ef71"), "height" : 220, "friends" : [ ObjectId("557e58727a8ea2a9dfe2ef74"), ObjectId("557e58727a8ea2a9dfe2ef77"), ObjectId("557e58727a8ea2a9dfe2ef7a"), ObjectId("557e58727a8ea2a9dfe2ef7e") ] },
  { "_id" : ObjectId("557e58727a8ea2a9dfe2ef7a"), "name" : "Rick", "postal_code" : ObjectId("557e56287a8ea2a9dfe2ef72"), "height" : 218, "friends" : [ ObjectId("557e58727a8ea2a9dfe2ef76"), ObjectId("557e58727a8ea2a9dfe2ef7c"), ObjectId("557e58727a8ea2a9dfe2ef7e") ] },
  { "_id" : ObjectId("557e58727a8ea2a9dfe2ef7c"), "name" : "Toby", "postal_code" : ObjectId("557e56287a8ea2a9dfe2ef72"), "height" : 214, "friends" : [ ObjectId("557e58727a8ea2a9dfe2ef7a") ] }
]
```

### Example 1
>下面的語法將會把 user 這個 collection 中身高大於 210 的使用者撈出，
>加入一個新欄位 num，值是同一個 postal_code 中身高大於 200 的使用者數，
>並且將其原本以 ObjectId 記錄的 postal_code 資料嵌入，
>再將 friends 中的 postal_code 嵌入。

```
@find <db=user coll=user query={ height : { $gte : 210 } }>
[
  @count (db=user coll=user query={ height : { $gte : 200 } , postal_code : @.postal_code })
  num

  @findOneById <db=info coll=postal_code projection={ _id : 0 , name : 1 }>
  postal_code

  @findOneById [db=user coll=user, projection={ _id : 0 , postal_code : 1, height : 1}]
  friends
  [
    @findOneById <db=info coll=postal_code projection={ _id : 0 , name : 1 }>
    postal_code
  ]
]
```

>執行 dsl 之後的結果

```
[
  {
    "_id":{
      "$oid":"557e58727a8ea2a9dfe2ef76"
    },
    "name":"Kirk",
    "postal_code":{
      "name":"中正區"
    },
    "height":220,
    "friends":[
      {
        "postal_code":{
          "name":"中正區"
        },
        "height":201
      },
      {
        "postal_code":{
          "name":"中正區"
        },
        "height":211
      },
      {
        "postal_code":{
          "name":"信義區"
        },
        "height":218
      },
      {
        "postal_code":{
          "name":"內湖區"
        },
        "height":208
      }
    ],
    "num":3
  },
  {
    "_id":{
      "$oid":"557e58727a8ea2a9dfe2ef77"
    },
    "name":"Mick",
    "postal_code":{
      "name":"中正區"
    },
    "height":211,
    "friends":[
      {
        "postal_code":{
          "name":"中正區"
        },
        "height":220
      }
    ],
    "num":3
  },
  {
    "_id":{
      "$oid":"557e58727a8ea2a9dfe2ef7a"
    },
    "name":"Rick",
    "postal_code":{
      "name":"信義區"
    },
    "height":218,
    "friends":[
      {
        "postal_code":{
          "name":"中正區"
        },
        "height":220
      },
      {
        "postal_code":{
          "name":"信義區"
        },
        "height":214
      },
      {
        "postal_code":{
          "name":"內湖區"
        },
        "height":208
      }
    ],
    "num":4
  },
  {
    "_id":{
      "$oid":"557e58727a8ea2a9dfe2ef7c"
    },
    "name":"Toby",
    "postal_code":{
      "name":"信義區"
    },
    "height":214,
    "friends":[
      {
        "postal_code":{
          "name":"信義區"
        },
        "height":218
      }
    ],
    "num":4
  }
]
```






# !!尚未完成 (!! Not Ready)

# 目標規格

## DSL語法
```
//用傳入的 String List 中的 Task id 填充 Task
@findOneById [db=user, coll=Task, projection={unit:true}]
<

  @findOneById [db=info, coll=Unit, projection={createDate:0}]
  unit <

    @findOne <query={_id:@}>
    subject

    @distinct (coll=Video key=subject query={subject:'PC',knowledge:{$in:@.knowledge}})
    @findOneById [coll=subject]
    videoSubject
    
    @findOneById [coll=Knowledge]
    knowledge [
        
      @findOneById <>
      subject
      
    ]

  >
  
  video <
  
    @findOne <query={_id:@}>
    subject
        
  >
  
  knowledge [
    
    @findOne <query={_id:@}>
    subject
    
    @findOne [query={_id:@}]
    child [
      
      @findOne <query={_id:@}>
      subject
      
    ]
    
  ]

>
```

## 支援的操作
### @find
* db
* coll
* projection
* query

### @findOne
* db
* coll
* projection
* query

### @findOneById
* db
* coll
* projection

### @distinct
* db
* coll
* key
* query

### @count
* db
* coll
* query

### @aggregate
* db
* coll
* pipelines


##作用域

* ( )  父層為 @
* < >  屬性值本身為 @
* \[ \]  屬性值底下的每個 item 為 @

若屬性值本身不是集合，則 \[ \] 和 { } 的 @ 指向同一個物件

```
{

  subject : "PC"

  video : [ "V_1" , "V_2" ]

  user : [ { _id : "U_1" , name : "N_1" } , { _id : "U_2" , name : "N_2" } ]

  link : { url : "URL" , display : "LINK" , next : { url : "NEXT_URL" , display : "NEXT" } }

}
```

作用域同時會影響各 action 的處理方式，
以上面的資料為基準

```
@findOne (query={"_id":"@"})
video

coll.findOne({"_id":{"subject":"PC","video":["V_1","V_2"],"user":[{"_id":"U_1","name":"N_1"},{"_id":"U_2","name":"N_2"}],"link":{"url":"URL","display":"LINK","next":{"url":"NEXT_URL","display":"NEXT"}}}})
```

```
@findOne <query={"_id":"@"}>
video

coll.findOne({"_id":["V_1","V_2"]})
```

```
@findOne [query={"_id":"@"}]
video

[
  coll.findOne({"_id":"V_1"}),
  coll.findOne({"_id":"V_2"})
]
```

```
@findOne [query={"_id":"@"}]
link

{
  url : coll.findOne({"_id":"URL"}),
  display : coll.findOne({"_id":"LINK"}),
  next : coll.findOne({"_id":{"url":"NEXT_URL","display":"NEXT"}})
}
```
