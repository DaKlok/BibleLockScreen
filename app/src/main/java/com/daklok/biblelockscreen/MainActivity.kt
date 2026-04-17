package com.daklok.biblelockscreen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.*
import coil.compose.rememberAsyncImagePainter
import coil.memory.MemoryCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.daklok.biblelockscreen.ui.theme.BibleLockScreenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

// --- TRANSLATIONS ---
data class AppStrings(
    val test: String = "Generovať",
    val generatingBtn: String = "Generuje sa...",
    val generating: String = "Tapeta sa generuje...",
    val done: String = "Hotovo",

    val updateTime: String = "Čas dennej aktualizácie",

    val tapToEdit: String = "Kliknutím upravíš polohu a veľkosť",
    val appName: String = "Bible Lock Screen",
    val settings: String = "Nastavenia aplikácie",
    val dailyWallpaper: String = "Denná zmena tapety",
    val active: String = "Aktívne (každý deň o %s:00)",
    val inactive: String = "Vypnuté",

    val textCustomization: String = "Prispôsobenie textu",
    val bold: String = "Tučné",
    val shadow: String = "Tieň",
    val textSize: String = "Veľkosť písma",
    val textWidth: String = "Šírka textu",
    val textHeight: String = "Výška textu",
    val textAlpha: String = "Priehľadnosť",
    val bgBlur: String = "Rozmazanie pozadia",
    val bgDarknessLabel: String = "Stmavenie pozadia",
    val anotherPhoto: String = "Iná fotka",
    val selectPhotoFirst: String = "Najskôr vyber fotku",
    val appearance: String = "Vzhľad",
    val system: String = "Systém",
    val light: String = "Svetlý",
    val dark: String = "Tmavý",
    val dynamicColor: String = "Material You (Farby systému)",
    val haptics: String = "Haptická odozva",
    val hapticsDesc: String = "Vibrácie pri upravovaní widgetu",
    val support: String = "Podpora",
    val supportDesc: String = "Ak sa ti aplikácia páči a chceš podporiť jej vývoj, môžeš mi kúpiť kávu ☕",
    val donate: String = "Prispieť",
    val close: String = "Zavrieť",
    val dragHint: String = "Ťahaj body pre veľkosť a šírku\nŤahaj do stredu pre presun",

    val cancel: String = "Zrušiť",
    val clickToSelect: String = "Klikni pre výber fotky",
    val loading: String = "Načítavam verš...",
    val appLanguage: String = "Jazyk aplikácie",
    val verseLanguage: String = "Jazyk veršov",
    val language: String = "Jazyk",
    val dailyWorkerOn: String = "Zapnuté!\nTapeta sa zmení zajtra o %s:00.",
    val dailyWorkerOff: String = "Denná zmena bola vypnutá.",
    val fontModern: String = "Moderný",
    val fontBook: String = "Knižný",

    val fontMono: String = "Strojový",
    val fontCursive: String = "Písaný",
    val fontLight: String = "Tenký",
    val fontCondensed: String = "Úzky",
    val langSk: String = "Slovenčina",
    val langEn: String = "English",
    val langCz: String = "Čeština",

    // Battery strings
    val batteryWarningTitle: String = "Upozornenie na batériu",
    val batteryWarningDesc: String = "Pre správne fungovanie automatickej zmeny tapety na pozadí, prosím, vypnite optimalizáciu batérie pre túto aplikáciu.",
    val batteryWarningButton: String = "Vypnúť optimalizáciu",

    // Custom Verse strings
    val customVerseTitle: String = "Vlastný text",
    val customVerseHint: String = "Text verša...",
    val customRefHint: String = "Súradnice (napr. Ján 3:16)",
    val applyCustom: String = "Použiť",

    // Automatic Wallpaper Change strings
    val autoWallpaper: String = "Automatická zmena tapety",
    val autoWallpaperIntervalLabel: String = "Interval zmeny",
    val autoWallpaperEvery1h: String = "Každú hodinu",
    val autoWallpaperEvery2h: String = "Každé 2 hodiny",
    val autoWallpaperEvery3h: String = "Každé 3 hodiny",
    val autoWallpaperEvery6h: String = "Každých 6 hodín",
    val autoWallpaperEvery12h: String = "Každých 12 hodín",
    val autoWallpaperEvery24h: String = "Každých 24 hodín (denne)",
    val autoWallpaperOnScreenOff: String = "Pri vypnutí obrazovky",
    val autoWallpaperOnScreenOffDesc: String = "Verš sa zmení pri každom zamknutí telefónu",
    val autoWallpaperTimeLabel: String = "Čas dennej zmeny",
    val autoWallpaperActiveHourly: String = "Aktívne (každých %sh)",
    val autoWallpaperActiveDaily: String = "Aktívne (denne o %s:00)",
    val autoWallpaperActiveScreenOff: String = "Aktívne (pri zamknutí)",
    val autoWorkerOn: String = "Zapnuté! Tapeta sa zmení podľa nastaveného intervalu.",
    val autoWorkerOff: String = "Automatická zmena bola vypnutá.",

    // Color picker
    val colorPickerTitle: String = "Vybrať farbu"
)

val skStrings = AppStrings()

val enStrings = AppStrings(
    updateTime = "Time of daily update",
    generatingBtn = "Generating...",
    tapToEdit = "Tap to edit position and size",
    settings = "App Settings",
    dailyWallpaper = "Daily Wallpaper Change",
    active = "Active (every day at %s:00)",
    inactive = "Disabled",
    textCustomization = "Text Customization",
    bold = "Bold",
    shadow = "Shadow",
    textSize = "Text Size",

    textWidth = "Text Width",
    textHeight = "Text Position",
    textAlpha = "Transparency",
    bgBlur = "Background Blur",
    bgDarknessLabel = "Background Darkness",
    anotherPhoto = "Change Photo",
    test = "Generate",
    selectPhotoFirst = "Select a photo first",
    appearance = "Appearance",
    system = "System",
    light = "Light",
    dark = "Dark",
    dynamicColor = "Material You (System Colors)",
    haptics = "Haptic Feedback",
    hapticsDesc = "Vibrations when editing the widget",
    support = "Support",

    supportDesc = "If you like the app and want to support its development, you can buy me a coffee ☕",
    donate = "Donate",
    close = "Close",
    dragHint = "Drag dots for size and width\nDrag the center to move",
    cancel = "Cancel",
    done = "Done",
    clickToSelect = "Tap to select photo",
    loading = "Loading verse...",
    appLanguage = "App Language",
    verseLanguage = "Verse Language",
    language = "Language",
    dailyWorkerOn = "Enabled!\nWallpaper will change tomorrow at %s:00.",
    dailyWorkerOff = "Daily change disabled.",
    generating = "Generating wallpaper...",
    fontModern = "Modern",
    fontBook = "Book",
    fontMono = "Typewriter",
    fontCursive = "Cursive",
    fontLight = "Light",
    fontCondensed = "Condensed",
    batteryWarningTitle = "Battery Warning",
    batteryWarningDesc = "For the automatic wallpaper change to work reliably in the background, please disable battery optimization for this app.",
    batteryWarningButton = "Disable Optimization",
    customVerseTitle = "Custom Text",
    customVerseHint = "Verse text...",
    customRefHint = "Coordinates (e.g. John 3:16)",
    applyCustom = "Apply",
    autoWallpaper = "Automatic Wallpaper Change",
    autoWallpaperIntervalLabel = "Change interval",
    autoWallpaperEvery1h = "Every hour",
    autoWallpaperEvery2h = "Every 2 hours",
    autoWallpaperEvery3h = "Every 3 hours",
    autoWallpaperEvery6h = "Every 6 hours",
    autoWallpaperEvery12h = "Every 12 hours",
    autoWallpaperEvery24h = "Every 24 hours (daily)",
    autoWallpaperOnScreenOff = "On screen off",
    autoWallpaperOnScreenOffDesc = "Verse changes every time you lock the phone",
    autoWallpaperTimeLabel = "Daily change time",
    autoWallpaperActiveHourly = "Active (every %sh)",
    autoWallpaperActiveDaily = "Active (daily at %s:00)",
    autoWallpaperActiveScreenOff = "Active (on lock)",
    autoWorkerOn = "Enabled! Wallpaper will change based on your interval.",
    autoWorkerOff = "Automatic change disabled.",
    colorPickerTitle = "Select color"
)

val czStrings = AppStrings(
    updateTime = "Čas denní aktualizace",
    settings = "Nastavení aplikace",
    dailyWallpaper = "Denní změna tapety",
    active = "Aktivní (každý den v %s:00)",
    inactive = "Vypnuto",
    textCustomization = "Přizpůsobení textu",
    bold = "Tučné",
    shadow = "Stín",
    textSize = "Velikost písma",
    textWidth = "Šířka textu",
    textHeight = "Výška textu",
    textAlpha = "Průhlednost",
    bgBlur = "Rozmazání pozadí",
    bgDarknessLabel = "Ztmavení pozadí",
    anotherPhoto = "Jiná fotka",
    test = "Generovat",
    selectPhotoFirst = "Nejdříve vyber fotku",
    appearance = "Vzhled",
    system = "Systém",
    light = "Světlý",
    dark = "Tmavý",
    dynamicColor = "Material You (Barvy systému)",
    haptics = "Haptická odezva",
    hapticsDesc = "Vibrace při úpravě widgetu",
    support = "Podpora",
    supportDesc = "Pokud se ti aplikace líbí a chceš podpořit její vývoj, můžeš mi koupit kávu ☕",
    donate = "Přispět",

    close = "Zavřít",
    dragHint = "Potažením bodů změň velikost\nTahem ve středu widget přesuň",
    cancel = "Zrušit",
    done = "Hotovo",
    clickToSelect = "Klikni pro výběr fotky",
    loading = "Načítám verš...",
    appLanguage = "Jazyk aplikace",
    verseLanguage = "Jazyk veršů",
    language = "Jazyk",
    dailyWorkerOn = "Zapnuto!\nTapeta se změní zítra v %s:00.",
    dailyWorkerOff = "Denní změna byla vypnuta.",
    generating = "Tapeta se generuje...",
    fontModern = "Moderní",

    fontBook = "Knižní",
    fontMono = "Strojový",
    fontCursive = "Psaný",
    fontLight = "Tenký",
    fontCondensed = "Úzký",
    generatingBtn = "Generuje se...",
    tapToEdit = "Kliknutím upravíš polohu a velikost",
    batteryWarningTitle = "Upozornění na baterii",
    batteryWarningDesc = "Aby automatická změna tapety fungovala spolehlivě na pozadí, vypněte prosím optimalizaci baterie pro tuto aplikaci.",
    batteryWarningButton = "Vypnout optimalizaci",
    customVerseTitle = "Vlastní text",
    customVerseHint = "Text verše...",
    customRefHint = "Souřadnice (např. Jan 3:16)",
    applyCustom = "Použít",
    autoWallpaper = "Automatická změna tapety",
    autoWallpaperIntervalLabel = "Interval změny",
    autoWallpaperEvery1h = "Každou hodinu",
    autoWallpaperEvery2h = "Každé 2 hodiny",
    autoWallpaperEvery3h = "Každé 3 hodiny",
    autoWallpaperEvery6h = "Každých 6 hodin",
    autoWallpaperEvery12h = "Každých 12 hodin",
    autoWallpaperEvery24h = "Každých 24 hodin (denně)",
    autoWallpaperOnScreenOff = "Při vypnutí obrazovky",
    autoWallpaperOnScreenOffDesc = "Verš se změní při každém zamknutí telefonu",
    autoWallpaperTimeLabel = "Čas denní změny",
    autoWallpaperActiveHourly = "Aktivní (každých %sh)",
    autoWallpaperActiveDaily = "Aktivní (denně v %s:00)",
    autoWallpaperActiveScreenOff = "Aktivní (při zamknutí)",
    autoWorkerOn = "Zapnuto! Tapeta se změní dle nastaveného intervalu.",
    autoWorkerOff = "Automatická změna byla vypnuta.",
    colorPickerTitle = "Vybrat barvu"
)

val esStrings = AppStrings(
    updateTime = "Hora de actualización diaria",
    settings = "Ajustes de la App",
    dailyWallpaper = "Cambio diario de fondo",
    active = "Activo (todos los días a las %s:00)",
    inactive = "Desactivado",
    textCustomization = "Personalización de texto",
    bold = "Negrita",
    shadow = "Sombra",
    textSize = "Tamaño del texto",
    textWidth = "Ancho del texto",
    textHeight = "Altura del texto",
    textAlpha = "Transparencia",

    bgBlur = "Desenfocar fondo",
    bgDarknessLabel = "Oscurecer el fondo",
    anotherPhoto = "Cambiar foto",
    test = "Generar",
    selectPhotoFirst = "Selecciona una foto primero",
    appearance = "Apariencia",
    system = "Sistema",
    light = "Claro",
    dark = "Oscuro",
    dynamicColor = "Material You (Colores del sistema)",
    haptics = "Respuesta háptica",
    hapticsDesc = "Vibraciones al editar el widget",
    support = "Soporte",
    supportDesc = "Si te gusta la app y quieres apoyar su desarrollo, puedes comprarme un café ☕",
    donate = "Donar",
    close = "Cerrar",
    dragHint = "Arrastra los puntos para el tamaño y ancho\nArrastra en el centro para mover",
    cancel = "Cancelar",
    done = "Hecho",
    clickToSelect = "Toca para seleccionar una foto",
    loading = "Cargando versículo...",
    appLanguage = "Idioma de la aplicación",
    verseLanguage = "Idioma del versículo",
    language = "Idioma",
    dailyWorkerOn = "¡Activado!\nEl fondo cambiará mañana a las %s:00.",

    dailyWorkerOff = "Cambio diario desactivado.",
    generating = "Generando fondo...",
    fontModern = "Moderno",
    fontBook = "Libro",
    fontMono = "Máquina de escribir",
    fontCursive = "Cursiva",
    fontLight = "Fino",
    fontCondensed = "Condensada",
    generatingBtn = "Generando...",
    tapToEdit = "Toca para editar la posición y el tamaño",
    batteryWarningTitle = "Advertencia de Batería",
    batteryWarningDesc = "Para que el cambio automático de fondo funcione de manera confiable, desactiva la optimización de batería para esta aplicación.",
    batteryWarningButton = "Desactivar Optimización",
    customVerseTitle = "Texto Personalizado",
    customVerseHint = "Texto del versículo...",
    customRefHint = "Coordenadas (ej. Juan 3:16)",
    applyCustom = "Aplicar",
    autoWallpaper = "Cambio automático de fondo",
    autoWallpaperIntervalLabel = "Intervalo de cambio",
    autoWallpaperEvery1h = "Cada hora",
    autoWallpaperEvery2h = "Cada 2 horas",
    autoWallpaperEvery3h = "Cada 3 horas",
    autoWallpaperEvery6h = "Cada 6 horas",
    autoWallpaperEvery12h = "Cada 12 horas",
    autoWallpaperEvery24h = "Cada 24 horas (diario)",
    autoWallpaperOnScreenOff = "Al apagar la pantalla",
    autoWallpaperOnScreenOffDesc = "El versículo cambia cada vez que bloqueas el teléfono",
    autoWallpaperTimeLabel = "Hora del cambio diario",
    autoWallpaperActiveHourly = "Activo (cada %sh)",
    autoWallpaperActiveDaily = "Activo (diario a las %s:00)",
    autoWallpaperActiveScreenOff = "Activo (al bloquear)",
    autoWorkerOn = "¡Activado! El fondo cambiará según el intervalo configurado.",
    autoWorkerOff = "Cambio automático desactivado.",
    colorPickerTitle = "Seleccionar color"
)

val itStrings = AppStrings(
    updateTime = "Ora della modifica giornaliera",
    settings = "Impostazioni App",
    dailyWallpaper = "Sfondo giornaliero",
    active = "Attivo (ogni giorno alle %s:00)",
    inactive = "Inattivo",
    textCustomization = "Personalizzazione testo",
    bold = "Grassetto",
    shadow = "Ombra",
    textSize = "Dimensione testo",
    textWidth = "Larghezza testo",
    textHeight = "Altezza testo",
    textAlpha = "Trasparenza",
    bgBlur = "Sfocatura sfondo",
    bgDarknessLabel = "Oscuramento dello sfondo",
    anotherPhoto = "Cambia foto",
    test = "Generare",
    selectPhotoFirst = "Seleziona prima una foto",
    appearance = "Aspetto",
    system = "Sistema",
    light = "Chiaro",
    dark = "Scuro",
    dynamicColor = "Material You (Colori di sistema)",
    haptics = "Feedback tattile",
    hapticsDesc = "Vibrazioni durante la modifica",
    support = "Supporto",
    supportDesc = "Se ti piace l'app e vuoi supportarne lo sviluppo, puoi offrirmi un caffè ☕",
    donate = "Dona",

    close = "Chiudi",
    dragHint = "Trascina i punti per la dimensione e la larghezza\nTrascina al centro per spostare",
    cancel = "Annulla",
    done = "Fatto",
    clickToSelect = "Tocca per selezionare una foto",
    loading = "Caricamento versetto...",
    appLanguage = "Lingua dell'app",
    verseLanguage = "Lingua del versetto",
    language = "Lingua",
    dailyWorkerOn = "Attivato!\nLo sfondo cambierà domani alle %s:00.",
    dailyWorkerOff = "Cambio giornaliero disattivato.",
    generating = "Generazione sfondo...",

    fontModern = "Moderno",
    fontBook = "Libro",
    fontMono = "Macchina da scrivere",
    fontCursive = "Corsivo",
    fontLight = "Sottile",
    fontCondensed = "Condensato",
    generatingBtn = "Generazione...",
    tapToEdit = "Tocca per cambiare posizione e dimensione",
    batteryWarningTitle = "Avviso Batteria",
    batteryWarningDesc = "Affinché il cambio automatico dello sfondo funzioni in modo affidabile, disabilita l'ottimizzazione della batteria per questa app.",
    batteryWarningButton = "Disabilita Ottimizzazione",
    customVerseTitle = "Testo Personalizzato",
    customVerseHint = "Testo del versetto...",
    customRefHint = "Coordinate (es. Giovanni 3:16)",
    applyCustom = "Applica",
    autoWallpaper = "Cambio automatico sfondo",
    autoWallpaperIntervalLabel = "Intervallo di cambio",
    autoWallpaperEvery1h = "Ogni ora",
    autoWallpaperEvery2h = "Ogni 2 ore",
    autoWallpaperEvery3h = "Ogni 3 ore",
    autoWallpaperEvery6h = "Ogni 6 ore",
    autoWallpaperEvery12h = "Ogni 12 ore",
    autoWallpaperEvery24h = "Ogni 24 ore (giornaliero)",
    autoWallpaperOnScreenOff = "Allo spegnimento schermo",
    autoWallpaperOnScreenOffDesc = "Il versetto cambia ogni volta che blocchi il telefono",
    autoWallpaperTimeLabel = "Orario cambio giornaliero",
    autoWallpaperActiveHourly = "Attivo (ogni %sh)",
    autoWallpaperActiveDaily = "Attivo (giornaliero alle %s:00)",
    autoWallpaperActiveScreenOff = "Attivo (al blocco)",
    autoWorkerOn = "Attivato! Lo sfondo cambierà in base all'intervallo impostato.",
    autoWorkerOff = "Cambio automatico disattivato.",
    colorPickerTitle = "Seleziona colore"
)

val frStrings = AppStrings(
    updateTime = "Heure de mise à jour quotidienne",
    settings = "Paramètres",
    dailyWallpaper = "Fond d'écran quotidien",
    active = "Actif (tous les jours à %s:00)",
    inactive = "Inactif",
    textCustomization = "Personnalisation du texte",
    bold = "Gras",
    shadow = "Ombre",
    textSize = "Taille du texte",
    textWidth = "Largeur du texte",
    textHeight = "Hauteur du texte",
    textAlpha = "Transparence",
    bgBlur = "Flou d'arrière-plan",
    bgDarknessLabel = "Assombrir l'arrière-plan",
    anotherPhoto = "Changer de photo",
    test = "Générer",
    selectPhotoFirst = "Sélectionnez d'abord une photo",
    appearance = "Apparence",
    system = "Système",
    light = "Clair",
    dark = "Sombre",
    dynamicColor = "Material You (Couleurs système)",
    haptics = "Retour haptique",
    hapticsDesc = "Vibrations lors de l'édition",
    support = "Soutien",
    supportDesc = "Si vous aimez l'application et souhaitez soutenir son développement, vous pouvez m'offrir un café ☕",

    donate = "Faire un don",
    close = "Fermer",
    dragHint = "Faites glisser les points pour la taille et la largeur\nFaites glisser au centre pour déplacer",
    cancel = "Annuler",
    done = "Terminé",
    clickToSelect = "Touchez pour sélectionner une photo",
    loading = "Chargement du verset...",
    appLanguage = "Langue de l'app",
    verseLanguage = "Langue du verset",
    language = "Langue",
    dailyWorkerOn = "Activé !\nLe fond d'écran changera demain à %s:00.",

    dailyWorkerOff = "Changement quotidien désactivé.",
    generating = "Génération du fond d'écran...",
    fontModern = "Moderne",
    fontBook = "Livre",
    fontMono = "Machine à écrire",
    fontCursive = "Cursive",
    fontLight = "Fin",
    fontCondensed = "Condensé",
    generatingBtn = "Génération...",
    tapToEdit = "Touchez pour modifier la position et la taille",
    batteryWarningTitle = "Avertissement de Batterie",
    batteryWarningDesc = "Pour que le changement automatique de fond d'écran fonctionne de manière fiable, veuillez désactiver l'optimisation de la batterie pour cette application.",
    batteryWarningButton = "Désactiver l'Optimisation",
    customVerseTitle = "Texte Personnalisé",
    customVerseHint = "Texte du verset...",
    customRefHint = "Coordonnées (ex. Jean 3:16)",
    applyCustom = "Appliquer",
    autoWallpaper = "Changement automatique du fond",
    autoWallpaperIntervalLabel = "Intervalle de changement",
    autoWallpaperEvery1h = "Chaque heure",
    autoWallpaperEvery2h = "Toutes les 2 heures",
    autoWallpaperEvery3h = "Toutes les 3 heures",
    autoWallpaperEvery6h = "Toutes les 6 heures",
    autoWallpaperEvery12h = "Toutes les 12 heures",
    autoWallpaperEvery24h = "Toutes les 24 heures (quotidien)",
    autoWallpaperOnScreenOff = "À l'extinction de l'écran",
    autoWallpaperOnScreenOffDesc = "Le verset change à chaque verrouillage du téléphone",
    autoWallpaperTimeLabel = "Heure du changement quotidien",
    autoWallpaperActiveHourly = "Actif (toutes les %sh)",
    autoWallpaperActiveDaily = "Actif (quotidien à %s:00)",
    autoWallpaperActiveScreenOff = "Actif (au verrouillage)",
    autoWorkerOn = "Activé ! Le fond changera selon l'intervalle configuré.",
    autoWorkerOff = "Changement automatique désactivé.",
    colorPickerTitle = "Choisir une couleur"
)

val deStrings = AppStrings(
    updateTime = "Zeitliche Aktualisierungszeit",
    settings = "App-Einstellungen",
    dailyWallpaper = "Tägliches Hintergrundbild",
    active = "Aktiv (jeden Tag bei %s:00)",
    inactive = "Inaktiv",
    textCustomization = "Textanpassung",
    bold = "Fett",
    shadow = "Schatten",
    textSize = "Textgröße",
    textWidth = "Textbreite",
    textHeight = "Texthöhe",
    textAlpha = "Transparenz",
    bgBlur = "Hintergrundunschärfe",
    bgDarknessLabel = "Hintergrund abdunkeln",
    anotherPhoto = "Anderes Foto",
    test = "Erzeugen",
    selectPhotoFirst = "Wähle zuerst ein Foto aus",
    appearance = "Erscheinungsbild",
    system = "System",
    light = "Hell",
    dark = "Dunkel",
    dynamicColor = "Material You (Systemfarben)",
    haptics = "Haptisches Feedback",
    hapticsDesc = "Vibrationen beim Bearbeiten",
    support = "Unterstützung",
    supportDesc = "Wenn dir die App gefällt und du ihre Entwicklung unterstützen möchtest, kannst du mir einen Kaffee spendieren ☕",
    donate = "Spenden",
    close = "Schließen",

    dragHint = "Ziehe die Punkte für die Größe und Breite\nZiehe in die Mitte zum Bewegen",
    cancel = "Abbrechen",
    done = "Fertig",
    clickToSelect = "Tippe, um ein Foto auszuwählen",
    loading = "Lade Vers...",
    appLanguage = "App-Sprache",
    verseLanguage = "Vers-Sprache",
    language = "Sprache",
    dailyWorkerOn = "Aktiviert!\nHintergrundbild ändert sich morgen um %s:00.",
    dailyWorkerOff = "Tägliche Änderung deaktiviert.",
    generating = "Hintergrundbild wird generiert...",
    fontModern = "Modern",

    fontBook = "Buch",
    fontMono = "Schreibmaschine",
    fontCursive = "Kursiv",
    fontLight = "Leicht",
    fontCondensed = "Schmal",
    generatingBtn = "Generieren...",
    tapToEdit = "Tippe, um die Position und Größe zu ändern",
    batteryWarningTitle = "Akku-Warnung",
    batteryWarningDesc = "Damit der automatische Hintergrundwechsel zuverlässig funktioniert, deaktivieren Sie bitte die Akku-Optimierung für diese App.",
    batteryWarningButton = "Optimierung Deaktivieren",
    customVerseTitle = "Eigener Text",
    customVerseHint = "Verstext...",
    customRefHint = "Koordinaten (z.B. Johannes 3:16)",
    applyCustom = "Anwenden",
    autoWallpaper = "Automatischer Hintergrundwechsel",
    autoWallpaperIntervalLabel = "Wechselintervall",
    autoWallpaperEvery1h = "Jede Stunde",
    autoWallpaperEvery2h = "Alle 2 Stunden",
    autoWallpaperEvery3h = "Alle 3 Stunden",
    autoWallpaperEvery6h = "Alle 6 Stunden",
    autoWallpaperEvery12h = "Alle 12 Stunden",
    autoWallpaperEvery24h = "Alle 24 Stunden (täglich)",
    autoWallpaperOnScreenOff = "Beim Ausschalten des Bildschirms",
    autoWallpaperOnScreenOffDesc = "Der Vers ändert sich bei jeder Sperrung",
    autoWallpaperTimeLabel = "Uhrzeit des täglichen Wechsels",
    autoWallpaperActiveHourly = "Aktiv (alle %sh)",
    autoWallpaperActiveDaily = "Aktiv (täglich um %s:00)",
    autoWallpaperActiveScreenOff = "Aktiv (bei Sperrung)",
    autoWorkerOn = "Aktiviert! Das Hintergrundbild wechselt nach dem eingestellten Intervall.",
    autoWorkerOff = "Automatischer Wechsel deaktiviert.",
    colorPickerTitle = "Farbe auswählen"
)

val huStrings = AppStrings(
    updateTime = "Napi frissítés időpontja",
    settings = "Alkalmazás beállításai",
    dailyWallpaper = "Napi háttérkép",
    active = "Aktív (minden nap %s:00)",
    inactive = "Inaktív",
    textCustomization = "Szöveg testreszabása",
    bold = "Félkövér",
    shadow = "Árnyék",
    textSize = "Szövegméret",
    textWidth = "Szöveg szélessége",
    textHeight = "Szöveg magassága",
    textAlpha = "Átlátszóság",
    bgBlur = "Háttér elmosása",
    bgDarknessLabel = "A háttér sötétítése",
    anotherPhoto = "Másik fotó",

    test = "Generálni",
    selectPhotoFirst = "Először válassz egy fotót",
    appearance = "Megjelenés",
    system = "Rendszer",
    light = "Világos",
    dark = "Sötét",
    dynamicColor = "Material You (Rendszerszínek)",
    haptics = "Haptikus visszajelzés",
    hapticsDesc = "Rezgés szerkesztéskor",
    support = "Támogatás",
    supportDesc = "Ha tetszik az alkalmazás és szeretnéd támogatni a fejlesztését, meghívhatsz egy kávéra ☕",
    donate = "Adomány",
    close = "Bezárás",

    dragHint = "Húzd a pontokat a méretért és szélességért\nHúzd a közepén a mozgatáshoz",
    cancel = "Mégse",
    done = "Kész",
    clickToSelect = "Koppints a fotó kiválasztásához",
    loading = "Ige betöltése...",
    appLanguage = "Alkalmazás nyelve",
    verseLanguage = "Ige nyelve",
    language = "Nyelv",
    dailyWorkerOn = "Bekapcsolva!\nA háttérkép holnap %s:00-kor változik.",
    dailyWorkerOff = "Napi módosítás kikapcsolva.",
    generating = "Háttérkép generálása...",
    fontModern = "Modern",
    fontBook = "Könyv",

    fontMono = "Írógép",
    fontCursive = "Dőlt",
    fontLight = "Vékony",
    fontCondensed = "Sűrített",
    generatingBtn = "Generálom...",
    tapToEdit = "Koppintson a pozíció és a méret módosításához.",
    batteryWarningTitle = "Akkumulátor Figyelmeztetés",
    batteryWarningDesc = "Annak érdekében, hogy az automatikus háttérkép-váltás megbízhatóan működjön, kérjük, tiltsa le az akkumulátor-optimalizálást ehhez az alkalmazáshoz.",
    batteryWarningButton = "Optimalizálás Letiltása",
    customVerseTitle = "Egyéni szöveg",
    customVerseHint = "Ige szövege...",
    customRefHint = "Koordináták (pl. János 3:16)",
    applyCustom = "Alkalmaz",
    autoWallpaper = "Automatikus háttérkép-csere",
    autoWallpaperIntervalLabel = "Csere időköze",
    autoWallpaperEvery1h = "Minden órában",
    autoWallpaperEvery2h = "Minden 2 órában",
    autoWallpaperEvery3h = "Minden 3 órában",
    autoWallpaperEvery6h = "Minden 6 órában",
    autoWallpaperEvery12h = "Minden 12 órában",
    autoWallpaperEvery24h = "Minden 24 órában (naponta)",
    autoWallpaperOnScreenOff = "Képernyő kikapcsolásakor",
    autoWallpaperOnScreenOffDesc = "Az ige minden zárolásnál változik",
    autoWallpaperTimeLabel = "Napi csere időpontja",
    autoWallpaperActiveHourly = "Aktív (minden %sh)",
    autoWallpaperActiveDaily = "Aktív (naponta %s:00-kor)",
    autoWallpaperActiveScreenOff = "Aktív (zárolásnál)",
    autoWorkerOn = "Bekapcsolva! A háttérkép a beállított időközönként változik.",
    autoWorkerOff = "Automatikus csere kikapcsolva.",
    colorPickerTitle = "Szín kiválasztása"
)

val plStrings = AppStrings(
    updateTime = "Czas aktualizacji dziennie",
    generatingBtn = "generowanie...",
    settings = "Ustawienia aplikacji",
    dailyWallpaper = "Codzienna tapeta",
    active = "Aktywne (codziennie o %s:00)",
    inactive = "Nieaktywne",
    textCustomization = "Dostosowanie tekstu",
    bold = "Pogrubienie",
    shadow = "Cień",
    textSize = "Rozmiar tekstu",
    textWidth = "Szerokość tekstu",
    textHeight = "Wysokość tekstu",
    textAlpha = "Przezroczystość",
    bgBlur = "Rozmycie tła",
    bgDarknessLabel = "Przyciemnianie tła",

    anotherPhoto = "Zmień zdjęcie",
    test = "Stwarzać",
    selectPhotoFirst = "Najpierw wybierz zdjęcie",
    appearance = "Wygląd",
    system = "System",
    light = "Jasny",
    dark = "Ciemny",
    dynamicColor = "Material You (Kolory systemu)",
    haptics = "Wibracje",
    hapticsDesc = "Wibracje podczas edycji",
    support = "Wsparcie",
    supportDesc = "Jeśli podoba Ci się aplikacja i chcesz wesprzeć jej rozwój, możesz postawić mi kawę ☕",
    donate = "Wesprzyj",

    close = "Zamknij",
    dragHint = "Przeciągnij kropki dla rozmiaru i szerokości\nPrzeciągnij środek, aby przesunąć",
    cancel = "Anuluj",
    done = "Gotowe",
    clickToSelect = "Dotknij, aby wybrać zdjęcie",
    loading = "Ładowanie wersetu...",
    appLanguage = "Język aplikacji",
    verseLanguage = "Język wersetu",
    language = "Język",
    dailyWorkerOn = "Włączone!\nTapeta zmieni się jutro o %s:00.",
    dailyWorkerOff = "Codzienna zmiana wyłączona.",
    generating = "Generowanie tapety...",
    fontModern = "Nowoczesny",

    fontBook = "Książka",
    fontMono = "Maszyna do pisania",
    fontCursive = "Kursywa",
    fontLight = "Cienki",
    fontCondensed = "Zwężony",
    tapToEdit = "Dotknij, aby zmienić położenie i wielkość",
    batteryWarningTitle = "Ostrzeżenie o Baterii",
    batteryWarningDesc = "Aby automatyczna zmiana tapety działała niezawodnie w tle, wyłącz optymalizację baterii dla tej aplikacji.",
    batteryWarningButton = "Wyłącz Optymalizację",
    customVerseTitle = "Własny tekst",
    customVerseHint = "Tekst wersetu...",
    customRefHint = "Współrzędne (np. Jan 3:16)",
    applyCustom = "Zastosuj",
    autoWallpaper = "Automatyczna zmiana tapety",
    autoWallpaperIntervalLabel = "Interwał zmiany",
    autoWallpaperEvery1h = "Co godzinę",
    autoWallpaperEvery2h = "Co 2 godziny",
    autoWallpaperEvery3h = "Co 3 godziny",
    autoWallpaperEvery6h = "Co 6 godzin",
    autoWallpaperEvery12h = "Co 12 godzin",
    autoWallpaperEvery24h = "Co 24 godziny (codziennie)",
    autoWallpaperOnScreenOff = "Po wyłączeniu ekranu",
    autoWallpaperOnScreenOffDesc = "Werset zmienia się przy każdym zablokowaniu telefonu",
    autoWallpaperTimeLabel = "Czas codziennej zmiany",
    autoWallpaperActiveHourly = "Aktywne (co %sh)",
    autoWallpaperActiveDaily = "Aktywne (codziennie o %s:00)",
    autoWallpaperActiveScreenOff = "Aktywne (przy blokowaniu)",
    autoWorkerOn = "Włączone! Tapeta zmieni się zgodnie z ustawionym interwałem.",
    autoWorkerOff = "Automatyczna zmiana wyłączona.",
    colorPickerTitle = "Wybierz kolor"
)

// Helper function to dynamically select system language or fallback to EN
fun getDefaultAppLanguage(): String {
    val sysLang = Locale.getDefault().language.uppercase()
    return when (sysLang) {
        "CS" -> "CZ"
        "SK", "EN", "CZ", "ES", "IT", "FR", "DE", "HU", "PL" -> sysLang
        else -> "EN"
    }
}

val availableLanguages = listOf(
    "EN" to "English",
    "SK" to "Slovenčina",
    "CZ" to "Čeština",

    "ES" to "Español",
    "IT" to "Italiano",
    "FR" to "Français",
    "DE" to "Deutsch",
    "HU" to "Magyar",
    "PL" to "Polski"
)

// --- CUSTOM IN-APP NOTIFICATION ---
enum class NotificationType { SUCCESS, INFO, ERROR }

data class AppNotification(
    val message: String,
    val type: NotificationType = NotificationType.SUCCESS,
    val id: Long = System.currentTimeMillis()
)

class MainActivity : ComponentActivity() {
    private lateinit var screenOffReceiver: ScreenOffReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register screen-off receiver dynamically (required for ACTION_SCREEN_OFF on API 26+)
        screenOffReceiver = ScreenOffReceiver()
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE) }


            var themeMode by remember { mutableIntStateOf(prefs.getInt("theme_mode", 0)) } // 0=System, 1=Light, 2=Dark
            var useDynamicColor by remember { mutableStateOf(prefs.getBoolean("use_dynamic_color", false)) }

            val darkTheme = when (themeMode) {
                1 -> false
                2 -> true

                else -> isSystemInDarkTheme()
            }

            BibleLockScreenTheme(darkTheme = darkTheme, dynamicColor = useDynamicColor) {
                MainScreen(
                    themeMode = themeMode,

                    useDynamicColor = useDynamicColor,
                    onThemeChange = { newMode ->
                        themeMode = newMode
                        prefs.edit().putInt("theme_mode", newMode).apply()
                    },

                    onDynamicColorChange = { useDynamic ->
                        useDynamicColor = useDynamic
                        prefs.edit().putBoolean("use_dynamic_color", useDynamic).apply()
                    }

                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(screenOffReceiver) } catch (e: Exception) { /* already unregistered */ }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    themeMode: Int,
    useDynamicColor: Boolean,
    onThemeChange: (Int) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val sysHaptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var activeNotification by remember { mutableStateOf<AppNotification?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as PowerManager }
    var isBatteryOptimized by remember { mutableStateOf(!powerManager.isIgnoringBatteryOptimizations(context.packageName)) }

    val scrollState = rememberScrollState()

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var versePair by remember { mutableStateOf<Pair<String, String>?>(null) }

    val prefs = remember { context.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE) }
    val defaultSystemLang = remember { getDefaultAppLanguage() }

    var bgDarkness by remember { mutableFloatStateOf(prefs.getFloat("bg_darkness", 0.23f)) }

    // --- STATES ---
    var hasSeenEditHint by remember { mutableStateOf(prefs.getBoolean("has_seen_edit_hint", false)) }
    var hasSeenSwipeHint by remember { mutableStateOf(prefs.getBoolean("has_seen_swipe_hint", false)) }

    LaunchedEffect(scrollState.value) {
        if (scrollState.value > 20 && !hasSeenSwipeHint) {
            hasSeenSwipeHint = true
            prefs.edit().putBoolean("has_seen_swipe_hint", true).apply()
        }
    }
    var dailyHour by remember { mutableIntStateOf(prefs.getInt("daily_hour", 6)) }
    var generationStatus by remember { mutableStateOf("idle") }

    var textSizeMult by remember { mutableFloatStateOf(prefs.getFloat("text_size_mult", 1.0f)) }
    var textWidthMult by remember { mutableFloatStateOf(prefs.getFloat("text_width_mult", 1.0f)) }
    var verticalOffset by remember { mutableFloatStateOf(prefs.getFloat("vertical_offset", 0.0f)) }
    var textColor by remember { mutableIntStateOf(prefs.getInt("text_color", AndroidColor.WHITE)) }
    var textAlpha by remember { mutableFloatStateOf(prefs.getFloat("text_alpha", 1.0f)) }
    var bgBlur by remember { mutableFloatStateOf(prefs.getFloat("bg_blur", 0f)) }
    var isBold by remember { mutableStateOf(prefs.getBoolean("is_bold", true)) }
    var useShadow by remember { mutableStateOf(prefs.getBoolean("use_shadow", true)) }
    var fontFamilyStr by remember { mutableStateOf(prefs.getString("font_family", "sans-serif") ?: "sans-serif") }
    var useHaptics by remember { mutableStateOf(prefs.getBoolean("use_haptics", true)) }

    var appLang by remember { mutableStateOf(prefs.getString("app_lang", defaultSystemLang) ?: defaultSystemLang) }
    var verseLang by remember { mutableStateOf(prefs.getString("verse_lang", defaultSystemLang) ?: defaultSystemLang) }

    // Strings
    val strings = when (appLang) {
        "EN" -> enStrings
        "CZ" -> czStrings
        "ES" -> esStrings
        "IT" -> itStrings
        "FR" -> frStrings
        "DE" -> deStrings
        "HU" -> huStrings
        "PL" -> plStrings
        else -> skStrings
    }

    // Edit Mode a Settings
    var isEditing by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var isDailyActive by remember { mutableStateOf(prefs.getBoolean("auto_wallpaper_active", false)) }
    var autoIntervalHours by remember { mutableIntStateOf(prefs.getInt("auto_interval_hours", 24)) }
    var changeOnScreenOff by remember { mutableStateOf(prefs.getBoolean("change_on_screen_off", false)) }

    // Custom Haptic Helper
    val performHaptic = { type: HapticFeedbackType ->
        if (useHaptics) {
            sysHaptic.performHapticFeedback(type)
        }
    }

    // Custom Notification Helper
    val showNotification = { message: String, type: NotificationType ->
        activeNotification = AppNotification(message = message, type = type)
        scope.launch {
            kotlinx.coroutines.delay(3000)
            activeNotification = null
        }
    }

    // Function to reload data so it catches background changes (like new verse or worker wallpaper change)
    val reloadPreviewData = {
        val useCustomVerse = prefs.getBoolean("use_custom_verse", false)
        val savedCustomVerse = prefs.getString("custom_verse_text", null)
        val savedCustomRef = prefs.getString("custom_verse_ref", null)

        if (useCustomVerse && !savedCustomVerse.isNullOrEmpty()) {
            versePair = Pair(savedCustomVerse, savedCustomRef ?: "")
        } else {
            scope.launch {
                versePair = LocalBibleProvider.getVerseForInterval(context, verseLang, autoIntervalHours)
            }
        }

        val localFile = java.io.File(context.filesDir, "user_wallpaper.jpg")
        if (localFile.exists()) {
            val internalUri = Uri.fromFile(localFile).buildUpon()
                .appendQueryParameter("v", System.currentTimeMillis().toString())
                .build()
            imageUri = internalUri
        } else {
            val savedUri = prefs.getString("bg_uri", null)
            if (savedUri != null) imageUri = Uri.parse(savedUri)
        }
    }

    // Obnovíme stav optimalizácie a preview dáta vždy, keď sa užívateľ vráti do aplikácie
    DisposableEffect(lifecycleOwner, verseLang) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isBatteryOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)
                reloadPreviewData() // Update UI in case midnight has passed while app was backgrounded
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        // Initial setup and background worker observer check
        reloadPreviewData()
    }

    // --- SAVE & ACTIONS ---
    fun saveSettings() {
        prefs.edit()
            .putFloat("text_size_mult", textSizeMult)
            .putFloat("text_width_mult", textWidthMult)
            .putFloat("vertical_offset", verticalOffset)
            .putInt("text_color", textColor)
            .putFloat("text_alpha", textAlpha)
            .putFloat("bg_blur", bgBlur)
            .putBoolean("is_bold", isBold)
            .putBoolean("use_shadow", useShadow)
            .putString("font_family", fontFamilyStr)
            .putBoolean("use_haptics", useHaptics)
            .putFloat("bg_darkness", bgDarkness)
            .apply()
    }

    fun toggleAutoWorker(enable: Boolean) {
        performHaptic(HapticFeedbackType.LongPress)
        saveSettings()
        isDailyActive = enable
        // Save master active state so ScreenOffReceiver can check it
        prefs.edit().putBoolean("auto_wallpaper_active", enable).apply()

        if (enable) {
            prefs.edit().putBoolean("use_custom_verse", false).apply()
            versePair = LocalBibleProvider.getVerseForInterval(context, verseLang, autoIntervalHours)
            if (!changeOnScreenOff) {
                scheduleAutoWallpaper(context, autoIntervalHours, dailyHour)
            }
            showNotification(strings.autoWorkerOn, NotificationType.SUCCESS)
        } else {
            WorkManager.getInstance(context).cancelUniqueWork("DailyBibleWallpaper")
            showNotification(strings.autoWorkerOff, NotificationType.INFO)
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { sourceUri ->
            scope.launch(Dispatchers.IO) {
                try {
                    val fileName = "user_wallpaper.jpg"
                    val destinationFile = java.io.File(context.filesDir, fileName)

                    context.contentResolver.openInputStream(sourceUri)?.use { input ->
                        destinationFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        val timestamp = System.currentTimeMillis()
                        val internalUriWithCacheBreaker = Uri.fromFile(destinationFile).buildUpon()
                            .appendQueryParameter("v", timestamp.toString())
                            .build()

                        imageUri = null
                        imageUri = internalUriWithCacheBreaker

                        val baseInternalUri = Uri.fromFile(destinationFile).toString()
                        prefs.edit().putString("bg_uri", baseInternalUri).apply()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    BackHandler(enabled = isEditing || showSettings) {
        if (isEditing) isEditing = false
        else if (showSettings) scope.launch {
            settingsSheetState.hide()
            showSettings = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                                        RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(strings.appName, fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        FilledIconButton(
                            onClick = { showSettings = true },
                            modifier = Modifier.padding(end = 8.dp).size(40.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Icon(Icons.Outlined.Settings, contentDescription = strings.settings, modifier = Modifier.size(20.dp))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            snackbarHost = { },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    }
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. DYNAMICKÝ PREVIEW CARD (Parallax efekt)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .graphicsLayer {
                            val scrollOffset = scrollState.value.toFloat()
                            val scale = (1f - (scrollOffset / 1500f)).coerceIn(0.6f, 1f)
                            val alphaVal = (1f - (scrollOffset / 900f)).coerceIn(0.5f, 1f)
                            scaleX = scale
                            scaleY = scale
                            alpha = alphaVal
                            translationY = scrollOffset * 0.5f
                        }
                ) {
                    Pixel6LockScreenPreview(
                        uri = imageUri,
                        verseText = versePair?.first ?: strings.loading,
                        verseReference = versePair?.second ?: "",
                        textSizeMult = textSizeMult,
                        textWidthMult = textWidthMult,
                        verticalOffset = verticalOffset,
                        textColor = textColor,
                        textAlpha = textAlpha,
                        bgBlur = bgBlur,
                        bgDarkness = bgDarkness,
                        isBold = isBold,
                        useShadow = useShadow,
                        fontFamilyStr = fontFamilyStr,
                        showEditHint = true, // Ukazuje obrys s ceruzkou
                        strings = strings,
                        showBubbleHint = imageUri != null && !hasSeenEditHint,
                        onClick = {
                            performHaptic(HapticFeedbackType.LongPress)
                            launcher.launch("image/*")
                        },
                        onEditClick = {
                            performHaptic(HapticFeedbackType.LongPress)
                            isEditing = true
                            if (!hasSeenEditHint) {
                                hasSeenEditHint = true
                                prefs.edit().putBoolean("has_seen_edit_hint", true).apply()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. OBLASŤ NASTAVENÍ
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow))
                        .padding(horizontal = 20.dp)
                        .padding(top = 8.dp, bottom = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Pill drag handle — bounces while user hasn't scrolled yet
                    val showSwipeHint = imageUri != null && !hasSeenSwipeHint && !isEditing
                    val infiniteTransition = rememberInfiniteTransition(label = "pill_bounce")
                    val pillOffsetY by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = if (showSwipeHint) -8f else 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pillOffsetY"
                    )
                    val pillWidth by infiniteTransition.animateFloat(
                        initialValue = 40f,
                        targetValue = if (showSwipeHint) 56f else 40f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pillWidth"
                    )
                    val pillAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.12f,
                        targetValue = if (showSwipeHint) 0.5f else 0.12f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pillAlpha"
                    )
                    Box(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .align(Alignment.CenterHorizontally)
                            .width(pillWidth.dp)
                            .height(4.dp)
                            .offset(y = pillOffsetY.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = pillAlpha),
                                RoundedCornerShape(2.dp)
                            )
                    )

                    // AUTOMATIC WALLPAPER CHANGE — MAIN TOGGLE CARD
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Icon badge
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        color = if (isDailyActive) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                        shape = RoundedCornerShape(14.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (isDailyActive) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            // Title + subtitle — weight(1f) so Switch is never overlapped
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.autoWallpaper,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isDailyActive) {
                                        when {
                                            changeOnScreenOff -> strings.autoWallpaperActiveScreenOff
                                            autoIntervalHours < 24 -> String.format(strings.autoWallpaperActiveHourly, autoIntervalHours)
                                            else -> String.format(strings.autoWallpaperActiveDaily, String.format("%02d", dailyHour))
                                        }
                                    } else {
                                        strings.inactive
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDailyActive) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Switch(
                                checked = isDailyActive,
                                onCheckedChange = { toggleAutoWorker(it) }
                            )
                        }
                    }

                    // AUTO WALLPAPER SUB-MENU (visible only when toggle is on)
                    AnimatedVisibility(
                        visible = isDailyActive,
                        enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(280)),
                        exit = shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) + fadeOut(tween(220))
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                            // ── INTERVAL ROW ─────────────────────────────────
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (changeOnScreenOff)
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {

                                    // Header: label on left, current value pill on right
                                    val intervalLabel = when (autoIntervalHours) {
                                        1 -> strings.autoWallpaperEvery1h
                                        2 -> strings.autoWallpaperEvery2h
                                        3 -> strings.autoWallpaperEvery3h
                                        6 -> strings.autoWallpaperEvery6h
                                        12 -> strings.autoWallpaperEvery12h
                                        else -> strings.autoWallpaperEvery24h
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = strings.autoWallpaperIntervalLabel,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (changeOnScreenOff) MaterialTheme.colorScheme.onSurfaceVariant
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                        // Pill badge showing selected value
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = if (changeOnScreenOff)
                                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                                    else
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(20.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = intervalLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (changeOnScreenOff)
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                else
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    // Slider snapping to: 1 2 3 6 12 24
                                    val intervalSteps = listOf(1, 2, 3, 6, 12, 24)
                                    val sliderIndex = intervalSteps.indexOf(autoIntervalHours).coerceAtLeast(0).toFloat()
                                    Slider(
                                        value = sliderIndex,
                                        onValueChange = { raw ->
                                            val idx = raw.roundToInt().coerceIn(0, intervalSteps.lastIndex)
                                            val hours = intervalSteps[idx]
                                            if (hours != autoIntervalHours) {
                                                autoIntervalHours = hours
                                                prefs.edit().putInt("auto_interval_hours", hours).apply()
                                                performHaptic(HapticFeedbackType.TextHandleMove)
                                                reloadPreviewData()
                                                if (isDailyActive && !changeOnScreenOff) {
                                                    scheduleAutoWallpaper(context, hours, dailyHour)
                                                }
                                            }
                                        },
                                        valueRange = 0f..(intervalSteps.lastIndex.toFloat()),
                                        steps = intervalSteps.lastIndex - 1,
                                        enabled = !changeOnScreenOff,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                            disabledThumbColor = MaterialTheme.colorScheme.outline,
                                            disabledActiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    // Tick labels below slider
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        intervalSteps.forEach { h ->
                                            Text(
                                                text = if (h < 24) "${h}h" else "24h",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (!changeOnScreenOff && autoIntervalHours == h)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (changeOnScreenOff) 0.4f else 0.7f),
                                                fontWeight = if (!changeOnScreenOff && autoIntervalHours == h) FontWeight.Bold else FontWeight.Normal,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }

                            // ── TIME OF DAY ROW (only for 24 h, non screen-off) ──
                            AnimatedVisibility(
                                visible = autoIntervalHours == 24 && !changeOnScreenOff,
                                enter = expandVertically(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(tween(200)),
                                exit = shrinkVertically(tween(180)) + fadeOut(tween(180))
                            ) {
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = strings.autoWallpaperTimeLabel,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                        RoundedCornerShape(20.dp)
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = String.format("%02d:00", dailyHour),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(12.dp))
                                        Slider(
                                            value = dailyHour.toFloat(),
                                            onValueChange = {
                                                dailyHour = it.roundToInt()
                                                performHaptic(HapticFeedbackType.TextHandleMove)
                                            },
                                            valueRange = 0f..23f,
                                            steps = 22,
                                            colors = SliderDefaults.colors(
                                                thumbColor = MaterialTheme.colorScheme.primary,
                                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                                inactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        LaunchedEffect(dailyHour) {
                                            prefs.edit().putInt("daily_hour", dailyHour).apply()
                                            if (isDailyActive && autoIntervalHours == 24 && !changeOnScreenOff) {
                                                scheduleDailyWallpaper(context, dailyHour)
                                            }
                                        }
                                    }
                                }
                            }

                            // ── ON SCREEN OFF ROW ────────────────────────────
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                color = if (changeOnScreenOff) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.TouchApp,
                                            contentDescription = null,
                                            tint = if (changeOnScreenOff) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = strings.autoWallpaperOnScreenOff,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = strings.autoWallpaperOnScreenOffDesc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2
                                        )
                                    }
                                    Switch(
                                        checked = changeOnScreenOff,
                                        onCheckedChange = { enabled ->
                                            changeOnScreenOff = enabled
                                            prefs.edit().putBoolean("change_on_screen_off", enabled).apply()
                                            performHaptic(HapticFeedbackType.LongPress)
                                            if (isDailyActive) {
                                                if (enabled) {
                                                    WorkManager.getInstance(context).cancelUniqueWork("DailyBibleWallpaper")
                                                } else {
                                                    scheduleAutoWallpaper(context, autoIntervalHours, dailyHour)
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }


                    // BATERIOVE UPOZORNENIE
                    AnimatedVisibility(
                        visible = isDailyActive && isBatteryOptimized,
                        enter = expandVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(300)),
                        exit = shrinkVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(250))
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = strings.batteryWarningTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = strings.batteryWarningDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                            context.startActivity(intent)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.onErrorContainer,
                                        contentColor = MaterialTheme.colorScheme.errorContainer
                                    )
                                ) {
                                    Text(strings.batteryWarningButton)
                                }
                            }
                        }
                    }

                    // CUSTOM VERSE EDITOR
                    AnimatedVisibility(
                        visible = !isDailyActive,
                        enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(280)),
                        exit = shrinkVertically(animationSpec = tween(220, easing = FastOutSlowInEasing)) + fadeOut(tween(220))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Outlined.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text(strings.customVerseTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }

                            var localVerse by remember(versePair) { mutableStateOf(versePair?.first ?: "") }
                            var localRef by remember(versePair) { mutableStateOf(versePair?.second ?: "") }

                            OutlinedTextField(
                                value = localVerse,
                                onValueChange = { localVerse = it },
                                label = { Text(strings.customVerseHint) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                minLines = 2,
                                maxLines = 4
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = localRef,
                                    onValueChange = { localRef = it },
                                    label = { Text(strings.customRefHint) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    singleLine = true
                                )

                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        if (localVerse.isNotBlank()) {
                                            versePair = Pair(localVerse, localRef)
                                            prefs.edit()
                                                .putString("custom_verse_text", localVerse)
                                                .putString("custom_verse_ref", localRef)
                                                .putBoolean("use_custom_verse", true)
                                                .apply()
                                            performHaptic(HapticFeedbackType.TextHandleMove)
                                            showNotification(strings.done, NotificationType.SUCCESS)
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.height(56.dp)
                                ) {
                                    Icon(Icons.Outlined.Check, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.applyCustom)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // NASTAVENIA
                    if (imageUri != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.TextFormat, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(strings.textCustomization, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }

                        // Farba textu
                        ColorPickerRow(selectedColor = textColor, strings = strings) { newColor ->
                            textColor = newColor
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            saveSettings()
                            if (imageUri != null) {
                                runOneTimeWorker(context)
                            }
                        }

                        // Písmo (Font)
                        FontPickerRow(selectedFont = fontFamilyStr, strings = strings) {
                            fontFamilyStr = it; performHaptic(HapticFeedbackType.TextHandleMove);
                            saveSettings()
                        }

                        // Štýly
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilterChip(
                                selected = isBold,
                                onClick = { isBold = !isBold; performHaptic(HapticFeedbackType.TextHandleMove); saveSettings() },
                                label = { Text(strings.bold) },
                                leadingIcon = { Icon(Icons.Outlined.FormatBold, null) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = useShadow,
                                onClick = { useShadow = !useShadow; performHaptic(HapticFeedbackType.TextHandleMove); saveSettings() },
                                label = { Text(strings.shadow) },
                                leadingIcon = { Icon(Icons.Default.Hd, null) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Slidery
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                EnhancedSlider(
                                    label = strings.bgBlur,
                                    value = bgBlur,
                                    range = 0f..25f,
                                    defaultVal = 0f,
                                    steps = 24,
                                    icon = Icons.Outlined.BlurOn,
                                    performHaptic = performHaptic,
                                    onValueChange = { bgBlur = it;
                                        saveSettings() }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                EnhancedSlider(
                                    label = strings.textSize,
                                    value = textSizeMult,
                                    range = 0.5f..2.0f,
                                    defaultVal = 1.0f,
                                    steps = 14,
                                    icon = Icons.Default.TextFormat,
                                    performHaptic = performHaptic,
                                    onValueChange = { textSizeMult = it; saveSettings() }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                EnhancedSlider(
                                    label = strings.textWidth,
                                    value = textWidthMult,
                                    range = 0.5f..1.2f,
                                    defaultVal = 1.0f,
                                    steps = 6,
                                    icon = Icons.Default.FormatAlignJustify,
                                    performHaptic = performHaptic,
                                    onValueChange = { textWidthMult = it; saveSettings() }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                EnhancedSlider(
                                    label = strings.textHeight,
                                    value = verticalOffset,
                                    range = -1.0f..1.0f,
                                    defaultVal = 0.0f,
                                    steps = 19,
                                    icon = Icons.Default.SwapVert,
                                    performHaptic = performHaptic,
                                    onValueChange = { verticalOffset = it; saveSettings() }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                EnhancedSlider(
                                    label = strings.textAlpha,
                                    value = textAlpha,
                                    range = 0.2f..1.0f,
                                    defaultVal = 1.0f,
                                    steps = 7,
                                    icon = Icons.Default.Opacity,
                                    performHaptic = performHaptic,
                                    onValueChange = { textAlpha = it;
                                        saveSettings() }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                EnhancedSlider(
                                    label = strings.bgDarknessLabel,
                                    value = bgDarkness,
                                    range = 0f..0.8f,
                                    defaultVal = 0.2f, //0.23 je default
                                    steps = 7,
                                    icon = Icons.Default.BrightnessMedium,
                                    performHaptic = performHaptic,
                                    onValueChange = {
                                        bgDarkness = it
                                        saveSettings()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Akčné tlačidlá
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { launcher.launch("image/*") },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.anotherPhoto, style = MaterialTheme.typography.labelLarge)
                            }

                            Button(
                                enabled = generationStatus == "idle" && imageUri != null,
                                onClick = {
                                    if (imageUri != null) {
                                        performHaptic(HapticFeedbackType.LongPress)
                                        scope.launch {
                                            generationStatus = "generating"
                                            runOneTimeWorker(context)
                                            kotlinx.coroutines.delay(2000)
                                            performHaptic(HapticFeedbackType.TextHandleMove)
                                            generationStatus = "Done"
                                            kotlinx.coroutines.delay(2000)
                                            generationStatus = "idle"
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                AnimatedContent(
                                    targetState = generationStatus == "generating",
                                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                                    label = "gen_btn"
                                ) { isGenerating ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (isGenerating) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                                        }
                                        Text(if (isGenerating) strings.generatingBtn else strings.test, style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clickable { launcher.launch("image/*") },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            ),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                                    Text(strings.selectPhotoFirst, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Nastavenia Aplikácie
        if (showSettings) {
            ModalBottomSheet(
                onDismissRequest = { showSettings = false },
                sheetState = settingsSheetState,
                dragHandle = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            BottomSheetDefaults.DragHandle()
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 8.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = strings.settings,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        settingsSheetState.hide()
                                        showSettings = false
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = strings.close,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    modifier = Modifier
                        .fillMaxHeight(0.92f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 40.dp)
                ) {

                    // --- LANGUAGE SECTION ---
                    SettingsSectionHeader(
                        icon = Icons.Default.Language,
                        title = strings.language
                    )
                    SettingsCard {
                        LanguageDropdown(
                            label = strings.appLanguage,
                            selectedCode = appLang,
                            options = availableLanguages,
                            showLabel = true,
                            onSelect = {
                                appLang = it
                                prefs.edit().putString("app_lang", it).apply()
                            }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        LanguageDropdown(
                            label = strings.verseLanguage,
                            selectedCode = verseLang,
                            options = availableLanguages,
                            showLabel = true,
                            onSelect = {
                                if (verseLang != it) {
                                    verseLang = it
                                    prefs.edit().putString("verse_lang", it).apply()
                                    versePair = null
                                    scope.launch {
                                        versePair = LocalBibleProvider.getVerseForInterval(context, it, autoIntervalHours)
                                    }
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // --- APPEARANCE SECTION ---
                    SettingsSectionHeader(
                        icon = Icons.Default.Palette,
                        title = strings.appearance
                    )
                    SettingsCard {
                        val themeOptions = listOf(strings.system, strings.light, strings.dark)
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            themeOptions.forEachIndexed { index, label ->
                                SegmentedButton(
                                    selected = themeMode == index,
                                    onClick = { onThemeChange(index) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = themeOptions.size)
                                ) {
                                    Text(label, maxLines = 1)
                                }
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.ColorLens,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(strings.dynamicColor, style = MaterialTheme.typography.bodyMedium)
                                }
                                Switch(checked = useDynamicColor, onCheckedChange = { onDynamicColorChange(it) })
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // --- HAPTICS SECTION ---
                    SettingsSectionHeader(
                        icon = Icons.Outlined.Fingerprint,
                        title = strings.haptics
                    )
                    SettingsCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.haptics, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(strings.hapticsDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = useHaptics,
                                onCheckedChange = {
                                    useHaptics = it
                                    saveSettings()
                                    if (it) sysHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // --- SUPPORT SECTION ---
                    SettingsSectionHeader(
                        icon = Icons.Outlined.Favorite,
                        title = strings.support
                    )
                    SettingsCard {
                        Text(
                            strings.supportDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/daklok"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF5E5B),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Outlined.Favorite, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.donate, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // Expressive Full-Screen Editor s animáciou
        AnimatedVisibility(
            visible = isEditing,
            enter = scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
            ) + fadeIn(animationSpec = tween(300)),
            exit = scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing)
            ) + fadeOut(animationSpec = tween(250))
        ) {
            FullScreenEditor(
                uri = imageUri,
                verseText = versePair?.first ?: strings.loading,
                verseReference = versePair?.second ?: "",
                initialTextSizeMult = textSizeMult,
                initialTextWidthMult = textWidthMult,
                initialVerticalOffset = verticalOffset,
                textColor = textColor,
                textAlpha = textAlpha,
                bgBlur = bgBlur,
                bgDarkness = bgDarkness,
                isBold = isBold,
                useShadow = useShadow,
                fontFamilyStr = fontFamilyStr,
                strings = strings,
                onSave = { newSize, newWidth, newOffset ->
                    textSizeMult = newSize
                    textWidthMult = newWidth
                    verticalOffset = newOffset
                    saveSettings()
                    isEditing = false
                },
                onDismiss = {
                    isEditing = false
                },
                performHaptic = performHaptic
            )
        }

        // Floating Status Popup
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            AnimatedVisibility(
                visible = generationStatus != "idle",
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }) + scaleIn(initialScale = 0.9f),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }) + scaleOut(targetScale = 0.9f)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 6.dp,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (generationStatus == "generating") {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(strings.generating, style = MaterialTheme.typography.labelLarge)
                        } else {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(strings.done, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // Custom In-App Notification Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            val rememberedNotification = remember { mutableStateOf<AppNotification?>(null) }
            if (activeNotification != null) rememberedNotification.value = activeNotification

            AnimatedVisibility(
                visible = activeNotification != null,
                enter = fadeIn(tween(300)) + slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                ) + scaleIn(initialScale = 0.9f, animationSpec = tween(300)),
                exit = fadeOut(tween(350)) + slideOutVertically(
                    targetOffsetY = { it / 2 },
                    animationSpec = tween(350, easing = FastOutLinearInEasing)
                ) + scaleOut(targetScale = 0.9f, animationSpec = tween(350))
            ) {
                rememberedNotification.value?.let { notification ->
                    val containerColor = when (notification.type) {
                        NotificationType.SUCCESS -> MaterialTheme.colorScheme.secondaryContainer
                        NotificationType.INFO -> MaterialTheme.colorScheme.secondaryContainer
                        NotificationType.ERROR -> MaterialTheme.colorScheme.errorContainer
                    }
                    val contentColor = when (notification.type) {
                        NotificationType.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
                        NotificationType.INFO -> MaterialTheme.colorScheme.onSecondaryContainer
                        NotificationType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
                    }
                    val notifIcon = when (notification.type) {
                        NotificationType.SUCCESS -> Icons.Outlined.Check
                        NotificationType.INFO -> Icons.Default.Info
                        NotificationType.ERROR -> Icons.Default.Warning
                    }
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = containerColor,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = notifIcon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = notification.message,
                                style = MaterialTheme.typography.labelLarge,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- KOMPONENTY ---

@Composable
fun LanguageDropdown(
    label: String,
    selectedCode: String,
    options: List<Pair<String, String>>,
    showLabel: Boolean = true,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.find { it.first == selectedCode }?.second ?: selectedCode

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showLabel) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(selectedLabel)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                options.forEach { (code, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            onSelect(code)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp, top = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun FullScreenEditor(
    uri: Uri?,
    verseText: String,
    verseReference: String,
    initialTextSizeMult: Float,
    initialTextWidthMult: Float,
    initialVerticalOffset: Float,
    textColor: Int,
    textAlpha: Float,
    bgBlur: Float,
    bgDarkness: Float,
    isBold: Boolean,
    useShadow: Boolean,
    fontFamilyStr: String,
    strings: AppStrings,
    onSave: (Float, Float, Float) -> Unit,
    onDismiss: () -> Unit,
    performHaptic: (HapticFeedbackType) -> Unit
) {
    val density = LocalDensity.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    // Lokálny stav
    var localSizeMult by remember { mutableFloatStateOf(initialTextSizeMult) }
    var rawSizeMult by remember { mutableFloatStateOf(initialTextSizeMult) }
    var isSizeSnapped by remember { mutableStateOf(kotlin.math.abs(initialTextSizeMult - 1.0f) < 0.05f) }

    var localWidthMult by remember { mutableFloatStateOf(initialTextWidthMult) }
    var rawWidthMult by remember { mutableFloatStateOf(initialTextWidthMult) }
    var isWidthSnapped by remember { mutableStateOf(kotlin.math.abs(initialTextWidthMult - 1.0f) < 0.05f) }

    var localVerticalOffset by remember { mutableFloatStateOf(initialVerticalOffset) }
    var rawVerticalOffset by remember { mutableFloatStateOf(initialVerticalOffset) }
    var isPositionSnapped by remember { mutableStateOf(kotlin.math.abs(initialVerticalOffset) < 0.04f) }

    // Konštanta pre rýchlosť vertikálneho posunu
    val moveFactor = 1f / (screenHeight.value * 0.35f)

    val updateWidth = { delta: Float ->
        rawWidthMult = (rawWidthMult + delta).coerceIn(0.5f, 1.5f)
        val snapZone = 0.05f
        val newSnapped = if (kotlin.math.abs(rawWidthMult - 1.0f) < snapZone) 1.0f else rawWidthMult
        if (newSnapped == 1.0f && !isWidthSnapped) {
            performHaptic(HapticFeedbackType.LongPress)
            isWidthSnapped = true
        } else if (newSnapped != 1.0f && isWidthSnapped) {
            isWidthSnapped = false
        }
        localWidthMult = newSnapped
    }

    val updateSize = { delta: Float ->
        rawSizeMult = (rawSizeMult + delta).coerceIn(0.5f, 3.0f)
        val snapZone = 0.08f
        val newSnapped = if (kotlin.math.abs(rawSizeMult - 1.0f) < snapZone) 1.0f else rawSizeMult
        if (newSnapped == 1.0f && !isSizeSnapped) {
            performHaptic(HapticFeedbackType.LongPress)
            isSizeSnapped = true
        } else if (newSnapped != 1.0f && isSizeSnapped) {
            isSizeSnapped = false
        }
        localSizeMult = newSnapped
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val maxWidth = maxWidth
        val maxHeight = maxHeight

        val screenWidthPx = with(density) { maxWidth.toPx() }
        val baseWidthPx = screenWidthPx * 0.80f

        // Výpočet veľkosti písma
        var baseSize = maxWidth.value * 0.055f
        if (verseText.length > 150) baseSize = maxWidth.value * 0.045f
        val fontScale = density.fontScale
        val fontSize = (baseSize * localSizeMult / fontScale).sp

        val composeFontFamily = getComposeFontFamily(fontFamilyStr)
        val composeFontWeight = getComposeFontWeight(fontFamilyStr, isBold)

        // Výpočet Y pozície
        val defaultOffset = maxHeight * 0.05f
        val variableOffset = (maxHeight * 0.35f) * localVerticalOffset
        val totalOffset = defaultOffset + variableOffset

        // 1. Pozadie
        if (uri != null) {

            Image(
                painter = rememberAsyncImagePainter(uri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (bgBlur > 0f) Modifier.blur((bgBlur * 0.7f).dp) else Modifier
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = bgDarkness))
            )
            Box(modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f)
                        )
                    )
                )
            )
        }

        // 2. FAKE STATUS BAR & CLOCK
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 60.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "09:41",
                color = Color(0xFFEEEEEE),
                fontSize = 72.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 2.sp
            )
            val date = SimpleDateFormat("EEE, d. MMM", Locale.getDefault()).format(Date())
            Text(
                text = date,
                color = Color(0xFFEEEEEE),
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // 3. EDITOVATEĽNÝ TEXT BOX
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val panYDp = dragAmount.y / density.density
                        val panOffsetChange = panYDp * moveFactor

                        rawVerticalOffset =
                            (rawVerticalOffset + panOffsetChange).coerceIn(-1.0f, 1.0f)

                        val snapZone = 0.04f
                        val newSnappedOffset = if (kotlin.math.abs(rawVerticalOffset) < snapZone) {
                            0.0f
                        } else {
                            rawVerticalOffset
                        }

                        if (newSnappedOffset == 0.0f && !isPositionSnapped) {
                            performHaptic(HapticFeedbackType.LongPress)
                            isPositionSnapped = true
                        } else if (newSnappedOffset != 0.0f && isPositionSnapped) {
                            isPositionSnapped = false
                        }

                        localVerticalOffset = newSnappedOffset
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Widget kontajner
            Box(
                modifier = Modifier
                    .offset(y = totalOffset)
                    .width(maxWidth * 0.80f * localWidthMult)
            ) {
                // Vnútorne ohraničený box s textom a pozadím
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isPositionSnapped) 3.dp else 2.dp, // Indikácia snapu stredovej pozície
                            color = if (isPositionSnapped) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .padding(horizontal = maxWidth * 0.025f, vertical = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = verseText,
                            color = Color(textColor).copy(alpha = textAlpha),
                            fontSize = fontSize,
                            fontFamily = composeFontFamily,
                            lineHeight = fontSize * 1.25f,
                            fontWeight = composeFontWeight,
                            textAlign = TextAlign.Center,
                            style = if (useShadow) TextStyleWithShadow else LocalTextStyle.current
                        )

                        Spacer(modifier = Modifier.height((fontSize.value * 0.5).dp))

                        Text(
                            text = verseReference,
                            color = Color(textColor).copy(alpha = textAlpha * 0.8f),
                            fontSize = fontSize * 0.75f,
                            fontFamily = composeFontFamily,
                            fontWeight = getComposeFontWeight(fontFamilyStr, false),
                            textAlign = TextAlign.Center,
                            style = if (useShadow) TextStyleWithShadow else LocalTextStyle.current
                        )
                    }
                }

                // DRAG HANDLES
                val handleSize = 52.dp
                val dotSize = 12.dp

                // Pravá strana (Šírka)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = 24.dp)
                        .size(handleSize)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val widthChangeMult = dragAmount.x / baseWidthPx
                                updateWidth(widthChangeMult * 2)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { Box(Modifier.size(dotSize).border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape).background(Color.White, CircleShape)) }

                // Ľavá strana (Šírka)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = -24.dp)
                        .size(handleSize)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val widthChangeMult = -dragAmount.x / baseWidthPx
                                updateWidth(widthChangeMult * 2)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { Box(Modifier.size(dotSize).border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape).background(Color.White, CircleShape)) }

                // Dolná strana (Veľkosť)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 24.dp)
                        .size(handleSize)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                updateSize(dragAmount.y / (200f * density.density))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { Box(Modifier.size(dotSize).border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape).background(Color.White, CircleShape)) }

                // Horná strana (Veľkosť)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = -24.dp)
                        .size(handleSize)
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                updateSize(-dragAmount.y / (200f * density.density))
                            }
                        },
                    contentAlignment = Alignment.Center
                ) { Box(Modifier.size(dotSize).border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape).background(Color.White, CircleShape)) }
            }
        }

        // 4. BOTTOM BAR WITH ACTIONS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                        startY = 0f
                    )
                )
                .padding(horizontal = 24.dp, vertical = 40.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = strings.dragHint,
                    style = MaterialTheme.typography.labelMedium.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 16f)
                    ),
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            performHaptic(HapticFeedbackType.LongPress)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.15f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = strings.cancel, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.cancel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            performHaptic(HapticFeedbackType.LongPress)
                            onSave(localSizeMult, localWidthMult, localVerticalOffset)
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = strings.done, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.done, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditHintBubble(text: String, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f, // Bounces up by 15dp
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounceOffset"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.offset(y = bounceOffset.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.TouchApp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
        }
        // Triangle pointing down at the verse
        Icon(
            Icons.Default.ArrowDropDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier
                .size(40.dp)
                .offset(y = (-14).dp) // Overlap to seamlessly connect to the bubble
        )
    }
}

@Composable
fun Pixel6LockScreenPreview(
    uri: Uri?,
    verseText: String,
    verseReference: String,
    textSizeMult: Float,
    textWidthMult: Float,
    verticalOffset: Float,
    textColor: Int,
    textAlpha: Float,
    bgBlur: Float,
    bgDarkness: Float,
    isBold: Boolean,
    useShadow: Boolean,
    fontFamilyStr: String,
    showEditHint: Boolean = false,
    showBubbleHint: Boolean = false,
    strings: AppStrings,
    onClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val density = LocalDensity.current
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val previewHeight = (screenHeight * 0.75f)

    Card(
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .height(previewHeight)
            .aspectRatio(9f / 20f)
            .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(32.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(12.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val maxWidth = maxWidth
            val maxHeight = maxHeight

            // Výpočet veľkosti písma
            var baseSize = maxWidth.value * 0.055f
            if (verseText.length > 150) baseSize = maxWidth.value * 0.045f
            val fontSize = (baseSize * textSizeMult / density.fontScale).sp
            val composeFontFamily = getComposeFontFamily(fontFamilyStr)
            val composeFontWeight = getComposeFontWeight(fontFamilyStr, isBold)

            // Výpočet Y pozície
            val defaultOffset = maxHeight * 0.05f
            val variableOffset = (maxHeight * 0.35f) * verticalOffset
            val totalOffset = defaultOffset + variableOffset

            // Pozadie
            if (uri != null) {
                Image(
                    painter = rememberAsyncImagePainter(uri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (bgBlur > 0f) Modifier.blur(bgBlur.dp) else Modifier
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = bgDarkness))
                )
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f)
                            )
                        )
                    )
                )
            } else {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF212122), Color(0xFF212121))
                        )
                    ), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(32.dp))
                        }
                        Text(strings.clickToSelect, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (uri != null) {
                // FAKE STATUS BAR & CLOCK (Pixel Style)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 60.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Clock
                    Text(
                        text = "09:41",
                        color = Color(0xFFEEEEEE),
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp
                    )
                    // Date
                    val date = SimpleDateFormat("EEE, d. MMM", Locale.getDefault()).format(Date())
                    Text(
                        text = date,
                        color = Color(0xFFEEEEEE),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // --- NÁHĽAD VERŠA ---
                // CenterBox pre náhľad a obal
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Vonkajší Box definuje presné rozmery celého orámovaného bloku textu aj na výšku vďaka intrinsic Content
                    Box(
                        modifier = Modifier
                            .offset(y = totalOffset)
                            .width(maxWidth * 0.80f * textWidthMult)
                            .clickable { onEditClick() }
                            .then(
                                if (showEditHint) Modifier.drawBehind {
                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.6f),
                                        style = Stroke(
                                            width = 1.5.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(
                                                floatArrayOf(
                                                    15f,
                                                    15f
                                                ), 0f
                                            )
                                        ),
                                        cornerRadius = CornerRadius(16.dp.toPx())
                                    )
                                } else Modifier
                            )
                    ) {

                        // Padded vnútorný obsah s textom - určuje výšku predchádzajúceho boxu
                        Box(modifier = Modifier.padding(horizontal = maxWidth * 0.025f, vertical = 16.dp)) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = verseText,
                                    color = Color(textColor).copy(alpha = textAlpha),
                                    fontSize = fontSize,
                                    fontFamily = composeFontFamily,
                                    lineHeight = fontSize * 1.25f,
                                    fontWeight = composeFontWeight,
                                    textAlign = TextAlign.Center,
                                    style = if (useShadow) TextStyleWithShadow else LocalTextStyle.current
                                )

                                Spacer(modifier = Modifier.height((fontSize.value * 0.5).dp))

                                Text(
                                    text = verseReference,
                                    color = Color(textColor).copy(alpha = textAlpha * 0.8f),
                                    fontSize = fontSize * 0.75f,
                                    fontFamily = composeFontFamily,
                                    fontWeight = getComposeFontWeight(fontFamilyStr, false),
                                    textAlign = TextAlign.Center,
                                    style = if (useShadow) TextStyleWithShadow else LocalTextStyle.current
                                )
                            }
                        }

                        // --- PRIDANÁ BUBLINA ---
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showBubbleHint,
                            enter = fadeIn(tween(400)) + scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                            exit = fadeOut(tween(300)) + scaleOut(targetScale = 0.8f),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                // Posunieme bublinu presne nad rámček s textom
                                .offset(y = (-70).dp)
                        ) {
                            EditHintBubble(text = strings.tapToEdit)
                        }

                        // Ikonka ceruzky
                        if (showEditHint) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    // Box je veľký 32.dp. Posunom o 16.dp ho vycentrujeme presne na roh.
                                    .offset(x = 16.dp, y = (-16).dp)
                                    .size(32.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                    .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Edit,
                                    contentDescription = "Editovať",
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // --- FINGERPRINT & BOTTOM ICONS ---
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 32.dp, start = 32.dp, end = 32.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Senzor odtlačku
                    Box(
                        modifier = Modifier
                            .padding(bottom = 100.dp)
                            .size(64.dp)
                            .background(Color(0x33FFFFFF), CircleShape)
                            .border(1.dp, Color(0x66FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Fingerprint, null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    // Ikony na krajoch
                    Icon(Icons.Default.PhotoCamera, null, tint = Color.White, modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp))
                    Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 16.dp))
                }
            }
        }
    }
}

@Composable
fun FontPickerRow(selectedFont: String, strings: AppStrings, onFontSelected: (String) -> Unit) {
    val fonts = listOf(
        "sans-serif" to strings.fontModern,
        "sans-serif-light" to strings.fontLight,
        "sans-serif-condensed" to strings.fontCondensed,
        "serif" to strings.fontBook,
        "monospace" to strings.fontMono,
        "cursive" to strings.fontCursive
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        fonts.forEach { (fontId, fontName) ->
            FilterChip(
                selected = selectedFont == fontId,
                onClick = { onFontSelected(fontId) },
                label = { Text(fontName, fontFamily = getComposeFontFamily(fontId), fontWeight = getComposeFontWeight(fontId, false)) },
                leadingIcon = if (selectedFont == fontId) {
                    { Icon(Icons.Default.Check, null) }
                } else null
            )
        }
    }
}

val TextStyleWithShadow = TextStyle(
    shadow = androidx.compose.ui.graphics.Shadow(
        color = Color.Black,
        blurRadius = 12f
    )
)

fun getComposeFontFamily(fontFamilyStr: String): FontFamily {
    return when (fontFamilyStr) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        "cursive" -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }
}

fun getComposeFontWeight(fontFamilyStr: String, isBold: Boolean): FontWeight {
    if (isBold) return FontWeight.Bold
    return when (fontFamilyStr) {
        "sans-serif-light" -> FontWeight.Light
        else -> FontWeight.Normal
    }
}

// ── Color picker helpers ──────────────────────────────────────────────────────

private fun hsvToComposeColor(hue: Float, sat: Float, value: Float): Color {
    val f = { n: Float ->
        val k = (n + hue / 60f) % 6f
        value - value * sat * maxOf(0f, minOf(k, 4f - k, 1f))
    }
    return Color(f(5f), f(3f), f(1f))
}

private fun composeColorToHsv(color: Color): Triple<Float, Float, Float> {
    val r = color.red; val g = color.green; val b = color.blue
    val max = maxOf(r, g, b); val mn = minOf(r, g, b); val d = max - mn
    val h = when {
        d == 0f  -> 0f
        max == r -> 60f * (((g - b) / d + 6f) % 6f)
        max == g -> 60f * ((b - r) / d + 2f)
        else     -> 60f * ((r - g) / d + 4f)
    }
    return Triple(h, if (max == 0f) 0f else d / max, max)
}

private fun Color.toHexStr(): String = String.format("%06X", toArgb() and 0xFFFFFF)

private fun hexStrToColor(hex: String): Color? {
    if (hex.length != 6) return null
    return try {
        val v = hex.toLong(16)
        Color(((v shr 16) and 0xFF) / 255f, ((v shr 8) and 0xFF) / 255f, (v and 0xFF) / 255f)
    } catch (_: NumberFormatException) { null }
}

private fun DrawScope.drawHsvWheel(cachedBitmap: androidx.compose.ui.graphics.ImageBitmap) {
    drawImage(cachedBitmap)
}

private fun buildWheelBitmap(sizePx: Int, value: Float): android.graphics.Bitmap {
    val bmp = android.graphics.Bitmap.createBitmap(sizePx, sizePx, android.graphics.Bitmap.Config.ARGB_8888)
    val cx = sizePx / 2f; val r = cx
    val pixels = IntArray(sizePx * sizePx)
    for (y in 0 until sizePx) {
        for (x in 0 until sizePx) {
            val dx = x - cx; val dy = y - cx
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)
            if (dist > r) { pixels[y * sizePx + x] = 0; continue }
            val hue = ((kotlin.math.atan2(dy, dx) * 180f / Math.PI.toFloat()) + 360f) % 360f
            val sat = (dist / r).coerceIn(0f, 1f)
            val c = hsvToComposeColor(hue, sat, value)
            pixels[y * sizePx + x] = android.graphics.Color.argb(
                255,
                (c.red * 255).toInt(),
                (c.green * 255).toInt(),
                (c.blue * 255).toInt()
            )
        }
    }
    bmp.setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
    return bmp
}

@Composable
private fun rememberWheelBitmap(sizePx: Int, value: Float): ImageBitmap? {
    val brightnessKey = (value * 50).toInt()
    val state = produceState<ImageBitmap?>(initialValue = null, sizePx, brightnessKey) {
        this.value = withContext(Dispatchers.Default) {
            buildWheelBitmap(sizePx, brightnessKey / 50f).asImageBitmap()
        }
    }
    return state.value
}

// ── ColorPickerRow ────────────────────────────────────────────────────────────

@Composable
fun ColorPickerRow(selectedColor: Int, strings: AppStrings, onColorSelected: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    val presetColors = listOf(
        AndroidColor.WHITE,
        AndroidColor.BLACK,
        AndroidColor.parseColor("#CFDEF3"), // soft blue-white
        AndroidColor.parseColor("#FFF8E7"), // warm cream
        AndroidColor.parseColor("#AAAAAA"), // mid gray
        AndroidColor.parseColor("#FFB347"), // golden
    )

    if (showDialog) {
        val initColor = Color(selectedColor)
        val (initH, initS, initV) = composeColorToHsv(initColor)

        var hueState   by remember { mutableStateOf(initH) }
        var satState   by remember { mutableStateOf(initS) }
        var valueState by remember { mutableStateOf(initV) }
        val pickedColor by remember(hueState, satState, valueState) {
            derivedStateOf { hsvToComposeColor(hueState, satState, valueState) }
        }
        var hexText by remember { mutableStateOf(initColor.toHexStr()) }
        var rText   by remember { mutableStateOf((initColor.red   * 255).toInt().toString()) }
        var gText   by remember { mutableStateOf((initColor.green * 255).toInt().toString()) }
        var bText   by remember { mutableStateOf((initColor.blue  * 255).toInt().toString()) }

        fun setHsv(h: Float, s: Float, v: Float) {
            hueState = h; satState = s; valueState = v
            val c = hsvToComposeColor(h, s, v)
            hexText = c.toHexStr()
            rText = (c.red * 255).toInt().toString()
            gText = (c.green * 255).toInt().toString()
            bText = (c.blue * 255).toInt().toString()
        }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(strings.colorPickerTitle) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Wheel + swatch row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Color wheel
                        Box(
                            modifier = Modifier
                                .size(150.dp)
                                .clip(CircleShape)
                                .pointerInput(valueState) {
                                    val sz = size.width.toFloat(); val cx = sz / 2f; val r = sz / 2f
                                    fun handle(pos: androidx.compose.ui.geometry.Offset) {
                                        val dx = pos.x - cx; val dy = pos.y - cx
                                        val dist = sqrt(dx * dx + dy * dy).coerceAtMost(r)
                                        val h = ((atan2(dy, dx) * 180f / Math.PI.toFloat()) + 360f) % 360f
                                        setHsv(h, dist / r, valueState)
                                    }
                                    detectTapGestures { handle(it) }
                                }
                                .pointerInput(valueState) {
                                    val sz = size.width.toFloat(); val cx = sz / 2f; val r = sz / 2f
                                    detectDragGestures { change, _ ->
                                        val pos = change.position
                                        val dx = pos.x - cx; val dy = pos.y - cx
                                        val dist = sqrt(dx * dx + dy * dy).coerceAtMost(r)
                                        val h = ((atan2(dy, dx) * 180f / Math.PI.toFloat()) + 360f) % 360f
                                        setHsv(h, dist / r, valueState)
                                    }
                                }
                        ) {
                            val density = LocalDensity.current
                            val wheelSizePx = with(density) { 150.dp.roundToPx() }
                            val wheelBitmap = rememberWheelBitmap(wheelSizePx, valueState)
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                if (wheelBitmap != null) drawImage(wheelBitmap)
                                val cx2 = size.width / 2f; val cy2 = size.height / 2f
                                val r2  = min(cx2, cy2)
                                val ang = hueState * Math.PI.toFloat() / 180f
                                val dotX = cx2 + satState * r2 * cos(ang)
                                val dotY = cy2 + satState * r2 * sin(ang)
                                drawCircle(Color.White,    radius = 9f,  center = androidx.compose.ui.geometry.Offset(dotX, dotY))
                                drawCircle(pickedColor,    radius = 6f,  center = androidx.compose.ui.geometry.Offset(dotX, dotY))
                            }
                        }

                        // Right column: swatch + brightness
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(pickedColor)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                            )
                            Text(
                                text = "\"For God so loved...\"",
                                fontSize = 11.sp,
                                color = pickedColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Brightness slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Brightness",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(70.dp)
                        )
                        Slider(
                            value = valueState,
                            onValueChange = { setHsv(hueState, satState, it) },
                            valueRange = 0f..1f,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${(valueState * 100).toInt()}%",
                            fontSize = 12.sp,
                            modifier = Modifier.width(36.dp)
                        )
                    }

                    // Hex input
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { raw ->
                            val clean = raw.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }.uppercase().take(6)
                            hexText = clean
                            if (clean.length == 6) {
                                hexStrToColor(clean)?.let { c ->
                                    val (h, s, v) = composeColorToHsv(c)
                                    hueState = h; satState = s; valueState = v
                                    rText = (c.red * 255).toInt().toString()
                                    gText = (c.green * 255).toInt().toString()
                                    bText = (c.blue * 255).toInt().toString()
                                }
                            }
                        },
                        label = { Text("Hex") },
                        prefix = { Text("#") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // RGB inputs
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        fun onRgbChange() {
                            val r2 = rText.toIntOrNull()?.coerceIn(0, 255) ?: return
                            val g2 = gText.toIntOrNull()?.coerceIn(0, 255) ?: return
                            val b2 = bText.toIntOrNull()?.coerceIn(0, 255) ?: return
                            val c  = Color(r2 / 255f, g2 / 255f, b2 / 255f)
                            val (h, s, v) = composeColorToHsv(c)
                            hueState = h; satState = s; valueState = v
                            hexText = c.toHexStr()
                        }
                        OutlinedTextField(
                            value = rText,
                            onValueChange = { rText = it.filter { c -> c.isDigit() }.take(3); onRgbChange() },
                            label = { Text("R") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = gText,
                            onValueChange = { gText = it.filter { c -> c.isDigit() }.take(3); onRgbChange() },
                            label = { Text("G") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = bText,
                            onValueChange = { bText = it.filter { c -> c.isDigit() }.take(3); onRgbChange() },
                            label = { Text("B") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Presets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            AndroidColor.WHITE,
                            AndroidColor.BLACK,
                            AndroidColor.parseColor("#CFDEF3"),
                            AndroidColor.parseColor("#FFF8E7"),
                            AndroidColor.parseColor("#AAAAAA"),
                            AndroidColor.parseColor("#FFB347"),
                            AndroidColor.parseColor("#87CEEB"),
                            AndroidColor.parseColor("#DDA0DD"),
                        ).forEach { preset ->
                            val pc = Color(preset)
                            val sel = pickedColor.toHexStr() == pc.toHexStr()
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(pc)
                                    .border(
                                        if (sel) 2.dp else 0.5.dp,
                                        if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape
                                    )
                                    .clickable {
                                        val (h, s, v) = composeColorToHsv(pc)
                                        setHsv(h, s, v)
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onColorSelected(pickedColor.toArgb())
                    showDialog = false
                }) { Text(strings.done) }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    // Quick preset swatches row (always visible)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        presetColors.forEach { color ->
            val isSelected = selectedColor == color
            val ringSize by animateDpAsState(
                targetValue = if (isSelected) 3.dp else 0.dp,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "ring"
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(ringSize, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                    .clickable { onColorSelected(color) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (color == AndroidColor.WHITE) Color.Black else Color.White
                    )
                }
            }
        }
        // Open full picker button
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.sweepGradient(
                        listOf(Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red)
                    )
                )
                .clickable { showDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ColorLens, "Custom Color", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun EnhancedSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    defaultVal: Float,
    steps: Int = 0,
    icon: ImageVector,
    performHaptic: (HapticFeedbackType) -> Unit,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format("%.1f", value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AnimatedVisibility(
                    visible = (value != defaultVal),
                    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
                ) {
                    IconButton(
                        onClick = {
                            performHaptic(HapticFeedbackType.LongPress)
                            onValueChange(defaultVal)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = "Reset",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        Slider(
            value = value,
            onValueChange = {
                val rounded = (it * 10).roundToInt() / 10f
                if (rounded != value) {
                    onValueChange(rounded)
                    performHaptic(HapticFeedbackType.TextHandleMove)
                }
            },
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            ),
            modifier = Modifier.padding(top = 0.dp)
        )
    }
}

// --- WORKER LOGIC ---

fun scheduleDailyWallpaper(context: Context, hour: Int) {
    val workManager = WorkManager.getInstance(context)

    val currentDate = Calendar.getInstance()
    val dueDate = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }

    if (dueDate.before(currentDate)) {
        dueDate.add(Calendar.HOUR_OF_DAY, 24)
    }

    val initialDelay = dueDate.timeInMillis - currentDate.timeInMillis

    val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyVerseWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .setConstraints(Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build())
        .build()

    workManager.enqueueUniquePeriodicWork(
        "DailyBibleWallpaper",
        ExistingPeriodicWorkPolicy.REPLACE,
        dailyWorkRequest
    )
}

/**
 * Schedules the wallpaper worker based on interval.
 * For 24h, aligns to the user-chosen time-of-day (cross-device sync via UTC epoch slot).
 * For shorter intervals, uses the interval directly — all devices on same slot because
 * epoch_hours / intervalHours gives the same slot number worldwide at the same UTC time.
 */
fun scheduleAutoWallpaper(context: Context, intervalHours: Int, dailyHour: Int) {
    val workManager = WorkManager.getInstance(context)

    if (intervalHours == 24) {
        scheduleDailyWallpaper(context, dailyHour)
        return
    }

    // For sub-day intervals: align initial delay to the next slot boundary
    // so all devices with the same interval are in sync (e.g. every 6h → 0,6,12,18 UTC)
    val nowEpochHours = System.currentTimeMillis() / (1000L * 60 * 60)
    val nextSlot = (nowEpochHours / intervalHours + 1) * intervalHours
    val nextSlotMs = nextSlot * 60L * 60L * 1000L
    val initialDelayMs = nextSlotMs - System.currentTimeMillis()

    val workRequest = PeriodicWorkRequestBuilder<DailyVerseWorker>(intervalHours.toLong(), TimeUnit.HOURS)
        .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
        .setConstraints(Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build())
        .build()

    workManager.enqueueUniquePeriodicWork(
        "DailyBibleWallpaper",
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}

// Helper to keep boot receiver / legacy code working
fun scheduleWorker(context: Context) {
    val prefs = context.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE)
    val savedHour = prefs.getInt("daily_hour", 6)
    val intervalHours = prefs.getInt("auto_interval_hours", 24)
    scheduleAutoWallpaper(context, intervalHours, savedHour)
}

fun runOneTimeWorker(context: Context) {
    val req = OneTimeWorkRequestBuilder<DailyVerseWorker>()
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .build()
    WorkManager.getInstance(context).enqueue(req)
}