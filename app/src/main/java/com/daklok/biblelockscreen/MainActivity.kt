package com.daklok.biblelockscreen

import android.Manifest
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.work.*
import coil.compose.rememberAsyncImagePainter
import com.daklok.biblelockscreen.ui.theme.BibleLockScreenTheme
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

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
    val textAlpha: String = "Priehľadnosť",
    val bgBlur: String = "Rozmazanie pozadia",
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
    val applyCustom: String = "Použiť"
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
    textAlpha = "Transparency",
    bgBlur = "Background Blur",
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
    applyCustom = "Apply"
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
    textAlpha = "Průhlednost",
    bgBlur = "Rozmazání pozadí",
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
    applyCustom = "Použít"
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
    textAlpha = "Transparencia",

    bgBlur = "Desenfocar fondo",
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
    applyCustom = "Aplicar"
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
    textAlpha = "Trasparenza",
    bgBlur = "Sfocatura sfondo",
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
    applyCustom = "Applica"
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
    textAlpha = "Transparence",
    bgBlur = "Flou d'arrière-plan",
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
    applyCustom = "Appliquer"
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
    textAlpha = "Transparenz",
    bgBlur = "Hintergrundunschärfe",
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
    applyCustom = "Anwenden"
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
    textAlpha = "Átlátszóság",
    bgBlur = "Háttér elmosása",
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
    applyCustom = "Alkalmaz"
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
    textAlpha = "Przezroczystość",
    bgBlur = "Rozmycie tła",

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
    applyCustom = "Zastosuj"
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE) }


            var themeMode by remember { mutableIntStateOf(prefs.getInt("theme_mode", 0)) } // 0=System, 1=Light, 2=Dark
            var useDynamicColor by remember { mutableStateOf(prefs.getBoolean("use_dynamic_color", true)) }

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

    // --- STATES ---
    var hasSeenEditHint by remember { mutableStateOf(prefs.getBoolean("has_seen_edit_hint", false)) }
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
    var isDailyActive by remember { mutableStateOf(false) }

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
                versePair = LocalBibleProvider.getVerse(context, verseLang)
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

        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkLiveData("DailyBibleWallpaper")
            .observeForever { infos ->
                isDailyActive = infos?.any {
                    it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
                } ?: false
            }
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
            .apply()
    }

    fun toggleDailyWorker(enable: Boolean) {
        performHaptic(HapticFeedbackType.LongPress)
        saveSettings()
        isDailyActive = enable

        if (enable) {
            prefs.edit().putBoolean("use_custom_verse", false).apply()
            versePair = LocalBibleProvider.getVerse(context, verseLang)

            scheduleDailyWallpaper(context, dailyHour)
            showNotification(
                String.format(strings.dailyWorkerOn, String.format("%02d", dailyHour)),
                NotificationType.SUCCESS
            )
        } else {
            WorkManager.getInstance(context).cancelUniqueWork("DailyBibleWallpaper")
            showNotification(strings.dailyWorkerOff, NotificationType.INFO)
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
                    title = { Text(strings.appName, fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Outlined.Settings, contentDescription = strings.settings)
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
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .animateContentSize(animationSpec = tween(300, easing = FastOutSlowInEasing))
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {

                    // HLAVNÝ PREPÍNAČ (SWITCH)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.dailyWallpaper,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isDailyActive) {
                                    val formattedHour = String.format("%02d", dailyHour)
                                    String.format(strings.active, formattedHour)
                                } else {
                                    strings.inactive
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDailyActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                        Switch(
                            checked = isDailyActive,
                            onCheckedChange = { toggleDailyWorker(it) }
                        )
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
                        enter = expandVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) + fadeIn(tween(300)),
                        exit = shrinkVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) + fadeOut(tween(250))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(strings.customVerseTitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

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

                    HorizontalDivider()

                    // NASTAVENIA
                    if (imageUri != null) {
                        Text(strings.textCustomization, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                        // Farba textu
                        ColorPickerRow(selectedColor = textColor) {
                            textColor = it; performHaptic(HapticFeedbackType.TextHandleMove);
                            saveSettings()
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

                        EnhancedSlider(
                            label = strings.textWidth,
                            value = textWidthMult,
                            range = 0.5f..1.2f,
                            defaultVal = 1.0f,
                            steps = 7,
                            icon = Icons.Default.FormatAlignJustify,
                            performHaptic = performHaptic,
                            onValueChange = { textWidthMult = it; saveSettings() }
                        )

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

                        Spacer(modifier = Modifier.height(12.dp))

                        // Akčné tlačidlá
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { launcher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Image, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.anotherPhoto)
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
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (generationStatus == "generating") {
                                        strings.generatingBtn
                                    } else {
                                        strings.test
                                    }
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .border(1.dp, Color.Gray, RoundedCornerShape(12.dp))
                                .clickable { launcher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.primary)
                                Text(strings.selectPhotoFirst, color = MaterialTheme.colorScheme.primary)
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
                                        versePair = LocalBibleProvider.getVerse(context, it)
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

                    // --- DAILY WALLPAPER SECTION ---
                    SettingsSectionHeader(
                        icon = Icons.Default.Schedule,
                        title = strings.dailyWallpaper
                    )
                    SettingsCard {
                        Text(
                            text = if (isDailyActive) {
                                val formattedHour = String.format("%02d", dailyHour)
                                String.format(strings.active, formattedHour)
                            } else {
                                strings.inactive
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDailyActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Slider(
                                value = dailyHour.toFloat(),
                                onValueChange = {
                                    dailyHour = it.roundToInt()
                                    if (useHaptics) performHaptic(HapticFeedbackType.TextHandleMove)
                                },
                                valueRange = 0f..23f,
                                steps = 22,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = String.format("%02d:00", dailyHour),
                                modifier = Modifier.padding(start = 12.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LaunchedEffect(dailyHour) {
                            prefs.edit().putInt("daily_hour", dailyHour).apply()
                            if (isDailyActive) {
                                scheduleDailyWallpaper(context, dailyHour)
                            }
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
                        NotificationType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
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
                val handleSize = 48.dp
                val dotSize = 10.dp

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
                ) { Box(Modifier.size(dotSize).background(Color.White, CircleShape)) }

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
                ) { Box(Modifier.size(dotSize).background(Color.White, CircleShape)) }

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
                ) { Box(Modifier.size(dotSize).background(Color.White, CircleShape)) }

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
                ) { Box(Modifier.size(dotSize).background(Color.White, CircleShape)) }
            }
        }

        // 4. BOTTOM BAR WITH ACTIONS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 40.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = strings.dragHint,
                    style = MaterialTheme.typography.labelMedium.copy(
                        shadow = androidx.compose.ui.graphics.Shadow(Color.Black, blurRadius = 12f)
                    ),
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            performHaptic(HapticFeedbackType.LongPress)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.DarkGray.copy(alpha = 0.8f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = strings.cancel)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.cancel, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            performHaptic(HapticFeedbackType.LongPress)
                            onSave(localSizeMult, localWidthMult, localVerticalOffset)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Outlined.Check, contentDescription = strings.done)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.done, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                    .background(Color(0xFF212121)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier
                            .size(48.dp)
                            .padding(bottom = 8.dp))
                        Text(strings.clickToSelect, color = Color.White.copy(alpha = 0.7f))
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

@Composable
fun ColorPickerRow(selectedColor: Int, onColorSelected: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val controller = rememberColorPickerController()

    val colors = listOf(
        AndroidColor.WHITE,
        AndroidColor.BLACK,
        AndroidColor.parseColor("#FFFFE0"), // Light Yellow
        AndroidColor.parseColor("#87CEEB"), // Sky Blue
        AndroidColor.parseColor("#FFB6C1"), // Light Pink
        AndroidColor.parseColor("#98FB98")  // Pale Green
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Vyberte farbu") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HsvColorPicker(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        controller = controller
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    BrightnessSlider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        controller = controller
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onColorSelected(controller.selectedColor.value.toArgb())
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Zrušiť")
                }
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .border(
                        width = if (selectedColor == color) 3.dp else 1.dp,
                        color = if (selectedColor == color) MaterialTheme.colorScheme.primary else Color.Gray,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(color) },
                contentAlignment = Alignment.Center
            ) {
                if (selectedColor == color) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = if (color == AndroidColor.WHITE || color == AndroidColor.parseColor("#FFFFE0")) Color.Black else Color.White
                    )
                }
            }
        }
        // Tlačidlo pre otvorenie Color Wheel
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Red,
                            Color.Magenta,
                            Color.Blue,
                            Color.Cyan,
                            Color.Green,
                            Color.Yellow,
                            Color.Red
                        )
                    )
                )
                .clickable { showDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ColorLens, "Custom Color", tint = Color.White)
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
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 0.dp)){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }

            // Value + Reset
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format("%.1f", value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                AnimatedVisibility(
                    visible = (value != defaultVal),
                    // This creates the "slide left" effect for the text
                    // and "fade/expand" effect for the button
                    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.Start),
                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.Start)
                ) {
                    IconButton(
                        onClick = {
                            performHaptic(HapticFeedbackType.LongPress)
                            onValueChange(defaultVal)
                        },
                        // Fixed size ensures the button doesn't stretch the row vertically
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = "Reset",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        Slider(
            value = value,
            onValueChange = {
                // Rounding to nearest 0.1
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
            )
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
        ExistingPeriodicWorkPolicy.REPLACE, // Restarts timer with new hour
        dailyWorkRequest
    )
}

// Helper to keep your existing toggle code working
fun scheduleWorker(context: Context) {
    val prefs = context.getSharedPreferences("bible_app_prefs", Context.MODE_PRIVATE)
    val savedHour = prefs.getInt("daily_hour", 6)
    scheduleDailyWallpaper(context, savedHour)
}

fun runOneTimeWorker(context: Context) {
    val req = OneTimeWorkRequestBuilder<DailyVerseWorker>()
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        .build()
    WorkManager.getInstance(context).enqueue(req)
}