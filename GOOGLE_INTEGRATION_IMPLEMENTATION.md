# Google Sheets & Cloud Storage Integration Implementation

## Overview
This implementation adds comprehensive Google Sheets and Google Cloud Storage integration to the Chavara Youth app, allowing users to fetch member data from Google Forms responses and automatically sync everything to cloud storage.

## Key Features Implemented

### 1. Google Sheets Integration
- **Service Account Authentication**: Uses `sheets_key.json` for secure API access
- **Spreadsheet URL Input**: Users can paste Google Sheets URLs in the SpreadsheetScreen
- **Data Parsing**: Automatically extracts member information from spreadsheet columns:
  - Column A: Name
  - Column B: Course
  - Column C: Birthday (DD/MM/YYYY format)
  - Column D: Phone Number
  - Column E: Residence
  - Column F: Email Address
  - Column G: How do you want to be part of Chavara Youth?
  - Column H: Photo URL (Google Drive link)
  - Column I: Video URL (optional)

### 2. Google Cloud Storage Integration
- **Service Account Authentication**: Uses `gcs_key.json` for secure GCS access
- **Automatic Data Sync**: All app data is automatically saved to cloud storage
- **Data Organization**: Structured storage in buckets:
  - `family-members/`: Individual member JSON files
  - `user-profile/`: User profile data
  - `app-settings/`: App configuration
  - `media/`: Photos and videos

### 3. Enhanced Data Models
- **Complete FamilyMember Model**: Includes all required fields plus helper methods
- **Birthday Management**: Methods to check today's birthdays and organize by month
- **Data Validation**: Proper date formatting and validation

### 4. Repository Pattern
- **ChavaraRepository**: Central data management with reactive StateFlow
- **Automatic Synchronization**: All changes are immediately synced to cloud
- **Offline Capability**: Local state management with cloud backup

### 5. UI Enhancements

#### SpreadsheetScreen
- URL input field with validation
- Loading states and error handling
- Success feedback when data is saved

#### HomeScreen
- Birthday notification section showing today's celebrants
- Dynamic layout that adapts based on birthday data
- Real-time updates from repository

#### FamilyMemberScreen
- Complete form with all data fields
- Cloud storage integration for saving members
- Support for both new and existing member editing

#### FamilyMembersListScreen
- Integration with repository for real-time data
- Dynamic member display based on actual data

#### GalleryMembersScreen
- Month-based organization of members
- Chronological sorting by birthday
- Display formatted dates for each member

#### AppResetScreen
- Confirmation dialog for data reset
- Complete cloud storage cleanup
- Local state reset

### 6. Gallery Management
- **GalleryManager**: Utility class for organizing photos/videos by month
- **Media Type Detection**: Automatic detection of image vs video content
- **Chronological Organization**: Members sorted by birthday within each month

## Security Features

### 1. Service Account Security
- JSON keys stored in `res/raw/` directory
- Credentials loaded securely using Android context
- Proper scope limitations for each service

### 2. ProGuard Obfuscation
- Enabled for release builds with resource shrinking
- Custom rules to protect Google API classes
- Preservation of data model classes for Gson serialization

### 3. Permissions
- Internet access for API calls
- Network state monitoring
- Minimal permission set for security

## Setup Instructions

### 1. Google Cloud Setup
1. Create a Google Cloud Project
2. Enable Google Sheets API and Cloud Storage API
3. Create two service accounts:
   - One for Google Sheets access
   - One for Google Cloud Storage access
4. Download the JSON key files and place them in `app/src/main/res/raw/`:
   - `sheets_key.json`
   - `gcs_key.json`

### 2. Google Cloud Storage Bucket
1. Create a bucket named `chavara-youth-data` (or update the bucket name in GoogleCloudStorageService.kt)
2. Set appropriate IAM permissions for the GCS service account

### 3. Google Sheets Permissions
1. Ensure the Sheets service account has read access to the target spreadsheets
2. Share the Google Form response sheet with the service account email

### 4. App Configuration
1. Update bucket name in `GoogleCloudStorageService.kt` if needed
2. Adjust column mappings in `GoogleSheetsService.kt` if your sheet structure differs
3. Build and install the app

## Usage Flow

1. **Initial Setup**: User navigates to SpreadsheetScreen
2. **Data Import**: Paste Google Sheets URL and tap Save
3. **Data Processing**: App fetches data and saves to cloud storage
4. **Data Display**: Members appear in Family Members screen and Gallery
5. **Birthday Notifications**: Today's birthdays show on home screen
6. **Ongoing Sync**: All changes automatically sync to cloud storage

## Data Synchronization

The app maintains perfect synchronization between local state and cloud storage:
- **Create**: New members saved locally and to cloud
- **Read**: Data loaded from cloud on app start
- **Update**: Member changes immediately synced
- **Delete**: Removals happen in both local state and cloud
- **Reset**: Complete cleanup of all data sources

## Error Handling

- Network connectivity checks
- Invalid URL validation
- Cloud storage access error handling
- Graceful fallbacks for missing data
- User-friendly error messages

## Performance Considerations

- Lazy loading of member data
- Efficient image/video loading strategies
- Background thread operations for API calls
- Memory-efficient data structures
- Proper lifecycle management

## Future Enhancements

- Image caching for better performance
- Offline mode with sync when connected
- Backup and restore functionality
- Data export capabilities
- Advanced search and filtering
- Push notifications for birthdays

This implementation provides a robust, scalable foundation for the Chavara Youth app with seamless cloud integration and data management.
