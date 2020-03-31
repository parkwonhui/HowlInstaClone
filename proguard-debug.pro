# Begin: Debug Proguard rules

-dontobfuscate                              #난독화를 수행하지 않도록 함
-keepattributes SoureFile,LineNumberTable   #소스파일, 라인 정보 유지
-keep class io.grpc.** {*;}
-dontwarn okio.**
-dontwarn retrofit2.**

# End: Debug ProGuard rules