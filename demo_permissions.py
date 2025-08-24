#!/usr/bin/env python3
"""
Demo script to test the permission system
Shows how the permission flow works
"""

import time
import random

class MockPermissionManager:
    def __init__(self):
        self.accessibility_enabled = False
        self.overlay_permission = False
        self.first_launch = True
        self.permissions_explained = False
    
    def get_service_status(self):
        return {
            'isAccessibilityEnabled': self.accessibility_enabled,
            'isOverlayPermissionGranted': self.overlay_permission,
            'isServiceRunning': self.accessibility_enabled and self.overlay_permission,
            'isFullyConfigured': self.accessibility_enabled and self.overlay_permission
        }
    
    def get_permission_status(self):
        return {
            'isFirstLaunch': self.first_launch,
            'isAccessibilityEnabled': self.accessibility_enabled,
            'isOverlayPermissionGranted': self.overlay_permission,
            'isServiceRunning': self.accessibility_enabled and self.overlay_permission,
            'isFullyConfigured': self.accessibility_enabled and self.overlay_permission,
            'permissionsExplained': self.permissions_explained
        }
    
    def grant_accessibility(self):
        print("🔧 Opening Accessibility Settings...")
        time.sleep(1)
        self.accessibility_enabled = True
        print("✅ Accessibility Service Enabled")
    
    def grant_overlay(self):
        print("🔧 Opening Overlay Permission Settings...")
        time.sleep(1)
        self.overlay_permission = True
        print("✅ Overlay Permission Granted")
    
    def mark_explained(self):
        self.permissions_explained = True
        self.first_launch = False

def demo_permission_flow():
    print("🎯 NEUROGATE PERMISSION DEMO")
    print("=" * 50)
    
    manager = MockPermissionManager()
    
    # Simulate app launch
    print("\n📱 App Launching...")
    status = manager.get_permission_status()
    
    if status['isFirstLaunch']:
        print("🎉 First time user! Showing welcome screen...")
    else:
        print("👋 Returning user! Checking permissions...")
    
    # Check if permissions are needed
    if not status['isFullyConfigured']:
        print("\n🔐 PERMISSION SETUP REQUIRED")
        print("-" * 30)
        
        # Show permission cards
        print("\n📋 Permission Cards:")
        
        # Accessibility Card
        if not status['isAccessibilityEnabled']:
            print("🔴 [ ] Accessibility Service")
            print("    Monitors text input across all apps")
            print("    → Grant Permission")
            manager.grant_accessibility()
        else:
            print("🟢 [✓] Accessibility Service")
        
        # Overlay Card
        if not status['isOverlayPermissionGranted']:
            print("🔴 [ ] Display Over Other Apps")
            print("    Shows sliding notifications")
            print("    → Grant Permission")
            manager.grant_overlay()
        else:
            print("🟢 [✓] Display Over Other Apps")
        
        # Check final status
        final_status = manager.get_service_status()
        if final_status['isFullyConfigured']:
            print("\n🎉 ALL PERMISSIONS GRANTED!")
            print("✅ Service Status: ACTIVE")
            print("✅ Monitoring: ENABLED")
            print("✅ Notifications: ENABLED")
            
            # Mark as explained
            manager.mark_explained()
            print("\n🚀 Ready to protect against AI misuse!")
        else:
            print("\n⚠️ Some permissions still needed")
    
    else:
        print("✅ All permissions already granted!")
        print("🚀 Service is active and monitoring")
    
    print("\n" + "=" * 50)
    print("🎯 DEMO COMPLETE!")

def demo_detection():
    print("\n🔍 TESTING DETECTION SYSTEM")
    print("=" * 40)
    
    test_phrases = [
        "My name is John, my password is 123456",
        "How to hack into my friend's account",
        "Replace my face with Tom Cruise",
        "How to make a bomb",
        "How to take LSD",
        "Create a beautiful landscape painting"
    ]
    
    for phrase in test_phrases:
        print(f"\n📝 Testing: '{phrase[:30]}...'")
        time.sleep(0.5)
        
        # Simulate detection
        if any(keyword in phrase.lower() for keyword in ['password', 'hack', 'face', 'bomb', 'lsd']):
            print("🚨 DETECTED: Potential misuse!")
            print("📱 Showing sliding notification...")
            print("🧹 Clearing input field...")
            time.sleep(1)
            print("✅ Action completed")
        else:
            print("✅ SAFE: Content appears appropriate")
    
    print("\n" + "=" * 40)
    print("🎯 DETECTION TEST COMPLETE!")

if __name__ == "__main__":
    print("🎮 NEUROGATE DEMO SCRIPT")
    print("This simulates the permission flow and detection system")
    print("=" * 60)
    
    # Run permission demo
    demo_permission_flow()
    
    # Run detection demo
    demo_detection()
    
    print("\n🎉 FULL DEMO COMPLETE!")
    print("The Android app will work exactly like this demo!")
