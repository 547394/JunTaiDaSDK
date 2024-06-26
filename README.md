## 引入

**Step 1.** Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:


	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}


**Step 2.** Add the dependency


	dependencies {
	        implementation 'com.github.547394:JuntaiDaSDK:1.0.3'
	}


## 最新版本

[![](https://jitpack.io/v/547394/JunTaiDaSDK.svg)](https://jitpack.io/#547394/JunTaiDaSDK)

## 使用

接口通讯示例请看[MainActivity.java](https://github.com/547394/JunTaiDaSDK/blob/495357af25fece6b2123eeae62ee87090a581e4a/app/src/main/java/com/jianxunfuture/juntaida/MainActivity.java)文件

MQTT通讯示例请看[MqttService.java](https://github.com/547394/JunTaiDaSDK/blob/495357af25fece6b2123eeae62ee87090a581e4a/app/src/main/java/com/jianxunfuture/juntaida/MqttService.java)文件
