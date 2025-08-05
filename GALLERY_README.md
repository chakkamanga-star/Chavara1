# Gallery Implementation

This document describes the implementation of the 4-screen gallery feature in the Chavara Android app.

## Overview

The gallery consists of 4 main screens:
1. **Gallery Main Screen** - Navigation hub with 3 options
2. **Gallery Photos Screen** - Grid view of photos with dynamic content
3. **Gallery Videos Screen** - Grid view of videos with dynamic content  
4. **Gallery Members Screen** - Grid view of member birthdays with dynamic content

## Screen Details

### 1. Gallery Main Screen (`GalleryMainScreen.kt`)
- Entry point accessed via orientation button on home screen
- Contains 3 navigation cards: "Cy Photos", "Cy Videos", "Members"
- Uses exact gradient background and styling from Figma design
- Background image (jesus.png) with 20% opacity overlay

### 2. Gallery Photos Screen (`GalleryPhotosScreen.kt`)
- 2-column grid layout displaying photo thumbnails
- Dynamic content support - grid items grow based on actual photo count
- Month subtitle ("January") with potential for dynamic month selection
- Each grid item has rounded corners with gradient background

### 3. Gallery Videos Screen (`GalleryVideosScreen.kt`)
- 2-column grid layout displaying video thumbnails
- Dynamic content support - grid items grow based on actual video count
- Month subtitle ("January") with potential for dynamic month selection
- Each grid item designed to hold video thumbnails

### 4. Gallery Members Screen (`GalleryMembersScreen.kt`)
- 2-column grid layout displaying member birthdays
- Special handling for first item showing day number "1"
- Dynamic content support - grid items grow based on actual member birthdays
- Title shows "Birthdays" to match the birthday theme

## Design Implementation

### Colors & Gradients
- **Background Gradient**: `#C38732` → `#DCB72F` → `#D3CA15` → `#DDE05F`
- **Grid Item Gradient**: `#CDD00E` → `#5E5300`
- **Text Color**: White (`#FFFFFF`)

### Typography
- **Main Titles**: 41-52sp, Righteous font family
- **Subtitles**: 29sp, Righteous font family
- **Grid Items**: 14-59sp depending on content

### Layout Specifications
- **Screen Size**: 412px × 917px
- **Grid Items**: 166dp × 183dp with 30dp border radius
- **Grid Spacing**: 30dp horizontal, 22dp vertical
- **Background Image**: 388dp × 690dp with 20% opacity

## Navigation Flow

```
HomeScreen (orientation button) 
    ↓
GalleryMainScreen
    ├── "Cy Photos" → GalleryPhotosScreen
    ├── "Cy Videos" → GalleryVideosScreen  
    └── "Members" → GalleryMembersScreen
```

## Dynamic Content Support

### Photos (`PhotoItem`)
```kotlin
data class PhotoItem(
    val id: String,
    val name: String,
    val imageUrl: String? = null
)
```

### Videos (`VideoItem`)
```kotlin
data class VideoItem(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val videoUrl: String? = null
)
```

### Members (`MemberItem`)
```kotlin
data class MemberItem(
    val id: String,
    val name: String,
    val birthday: String? = null,
    val imageUrl: String? = null
)
```

## Usage

The gallery screens automatically handle:
- **Dynamic grid expansion** - as you add more photos/videos/members, the grid grows
- **Month-based organization** - current implementation shows "January", easily extensible
- **Proper navigation** - back button support and proper navigation stack
- **Exact design matching** - all colors, spacing, and styling match the Figma design

## Future Enhancements

1. **Month Selection** - Add month picker to filter content by month
2. **Image Loading** - Integrate with actual image/video loading libraries
3. **Database Integration** - Connect to local database or API for real content
4. **Search & Filter** - Add search functionality within each gallery type
5. **Full-Screen View** - Add detailed view when tapping on grid items

## Files Structure

```
app/src/main/java/com/sj9/chavara/
├── navigation/
│   └── AppNavigation.kt          # Navigation setup
├── ui/
│   ├── HomeScreen.kt            # Updated with orientation click
│   └── gallery/
│       ├── GalleryMainScreen.kt     # Main gallery hub
│       ├── GalleryPhotosScreen.kt   # Photos grid
│       ├── GalleryVideosScreen.kt   # Videos grid
│       └── GalleryMembersScreen.kt  # Members grid
└── MainActivity.kt               # Updated to use navigation
```

## Dependencies Added

- `androidx.navigation:navigation-compose` - For screen navigation
- Background image: `res/drawable/jesus.png` - Used across all gallery screens
