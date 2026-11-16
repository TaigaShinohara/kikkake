# WorkManager workers and receivers are instantiated by name/reflection
-keep class com.taigashinohara.kikkake.MonitorWorker { *; }
-keep class com.taigashinohara.kikkake.PowerConnectedReceiver { *; }
-keep class com.taigashinohara.kikkake.AlarmReceiver { *; }
-keep class com.taigashinohara.kikkake.BootReceiver { *; }
