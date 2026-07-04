# R8 / ProGuard rules for release builds.
#
# The domain layer is plain Kotlin with no reflection, so it needs no special
# keeps — R8 is free to shrink and rename it aggressively. Add keeps below only
# for types accessed reflectively (kotlinx.serialization models, Room entities,
# Hilt-generated code is handled by the Hilt consumer rules automatically).

# Keep Timber's line-number info readable in obfuscated stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin metadata (needed by some reflective libraries and better stack traces).
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# --- Placeholders for later phases -----------------------------------------
# Phase 3 (Room + kotlinx.serialization for the signed save):
# -keep class gt.guardian.cadejo.core.data.save.** { *; }
# Phase 4/5 (Supabase DTOs, Billing/AdMob) will add their own consumer rules.
