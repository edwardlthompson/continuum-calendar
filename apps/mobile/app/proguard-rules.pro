-keep class org.fossify.calendar.models.** { *; }
# Tink (security-crypto) references this compile-only annotation.
-dontwarn javax.annotation.concurrent.GuardedBy
