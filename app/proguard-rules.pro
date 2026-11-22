# WorkManager workers and receivers are instantiated by name/reflection
-keep class com.taigashinohara.kikkake.MonitorWorker { *; }
-keep class com.taigashinohara.kikkake.PowerConnectedReceiver { *; }
-keep class com.taigashinohara.kikkake.AlarmReceiver { *; }
-keep class com.taigashinohara.kikkake.BootReceiver { *; }
# Rhino（JavaScript実行）参照する java.beans は Android に無いため R8 の警告を止める（v2.2.0）
-dontwarn java.beans.BeanDescriptor
-dontwarn java.beans.BeanInfo
-dontwarn java.beans.IntrospectionException
-dontwarn java.beans.Introspector
-dontwarn java.beans.PropertyDescriptor
