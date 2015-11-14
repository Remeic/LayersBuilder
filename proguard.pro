-libraryjars <java.home>/lib/rt.jar

-dontobfuscate

-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe
-dontwarn com.google.common.collect.MinMaxPriorityQueue
-dontwarn com.google.common.**

-keepclasseswithmembers public class * {
    public static void main(java.lang.String[]);
}