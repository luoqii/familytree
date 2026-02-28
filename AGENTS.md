# 总览
家庭树，用于记录，浏览族谱信息

## 数据模型
基本单位是人（persion）  
每个人有如下信息：
* 唯一ID，一旦生成，不会改变，即使这个数据被删除
  * 防止ID推导，ID生成规则可参考[familysearch](https://www.familysearch.org/)的规则
* 姓和名
* 性别
* 出生日期
* 出生地点
* 死亡日期
* 死亡地点
* 父亲
* 母亲
* 备注


## 目录结构
android android app 专用目录  
ios ios app专用目录  
server 后端逻辑目录  

## 编码风格
java 遵循google 的java 风格  
kotlin 使用官方kotlin 风格

## 代码相关
* 每一次代码的调整，都应该涉及单元测试的调整
* 每一次代码调整，都要求编译成功，自动化测试能正常完成
* 复杂代码需要有简洁，明晰的注释
