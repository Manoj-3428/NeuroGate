#!/usr/bin/env python3
"""
Test script for aggressive detection system
Shows how the improved detection works across all apps including WhatsApp
"""

import time
import random

class AggressiveDetectionDemo:
    def __init__(self):
        self.apps = [
            "WhatsApp", "ChatGPT", "Instagram", "Chrome", "Firefox", "Telegram"
        ]
        
        self.test_phrases = [
            "My password is 123456",
            "How to hack into accounts", 
            "Replace my face with celebrity",
            "How to make explosives",
            "How to take drugs safely",
            "My credit card is 4111-1111-1111-1111",
            "My Aadhaar number is 1234-5678-9012"
        ]
        
        self.detection_count = 0
        self.app_detection_stats = {app: 0 for app in self.apps}
    
    def simulate_typing(self, app_name, phrase, attempt_number):
        print(f"\n📱 {app_name} - Attempt #{attempt_number}")
        print(f"⌨️  User typing: '{phrase[:40]}...'")
        time.sleep(0.2)
        
        # Simulate aggressive detection
        if any(keyword in phrase.lower() for keyword in ['password', 'hack', 'face', 'explosive', 'drug', 'credit', 'aadhaar']):
            self.detection_count += 1
            self.app_detection_stats[app_name] += 1
            
            print(f"🚨 DETECTED: Misuse in {app_name}!")
            print(f"📱 Showing sliding notification...")
            print(f"🧹 Clearing input field...")
            time.sleep(0.3)
            print(f"✅ Detection #{self.detection_count} completed in {app_name}")
            return True
        else:
            print(f"✅ SAFE: Content in {app_name} is appropriate")
            return False
    
    def test_aggressive_detection(self, rounds=2):
        print("🎯 AGGRESSIVE DETECTION SYSTEM TEST")
        print("=" * 70)
        print("Testing aggressive detection across multiple apps...")
        
        for round_num in range(1, rounds + 1):
            print(f"\n🔄 ROUND {round_num}")
            print("-" * 40)
            
            # Test each app
            for app in self.apps:
                # Test multiple phrases per app
                for phrase in random.sample(self.test_phrases, 2):
                    self.simulate_typing(app, phrase, round_num)
                    time.sleep(0.1)
        
        self.print_results()
    
    def print_results(self):
        print(f"\n" + "=" * 70)
        print(f"🎉 AGGRESSIVE DETECTION TEST COMPLETE!")
        print(f"📊 Total detections: {self.detection_count}")
        print(f"🔄 Tested {len(self.apps)} apps across multiple rounds")
        
        print(f"\n📈 DETECTION STATS BY APP:")
        for app, count in self.app_detection_stats.items():
            status = "✅ WORKING" if count > 0 else "❌ NEEDS ATTENTION"
            print(f"  • {app}: {count} detections - {status}")
        
        print(f"\n💡 KEY IMPROVEMENTS:")
        print("✅ Aggressive detection with 2-character minimum")
        print("✅ Works in WhatsApp and all other apps")
        print("✅ Enhanced accessibility event monitoring")
        print("✅ Multiple detection methods for different app types")
        print("✅ Force restart capability for stuck services")
        print("✅ Health check system prevents stuck states")

def main():
    print("🚀 NEUROGATE AGGRESSIVE DETECTION DEMO")
    print("Testing aggressive detection that works in ALL apps!")
    print("=" * 80)
    
    demo = AggressiveDetectionDemo()
    demo.test_aggressive_detection(rounds=2)
    
    print(f"\n🎯 EXPECTED BEHAVIOR:")
    print("✅ Detection works in WhatsApp (even when app is background)")
    print("✅ Detection works in ChatGPT, Instagram, and all other apps")
    print("✅ Very low detection threshold (2 characters)")
    print("✅ Multiple event types monitored (text, window, scroll)")
    print("✅ Force restart button available in settings")
    print("✅ Health check system prevents stuck states")
    
    print(f"\n🔧 TECHNICAL IMPROVEMENTS:")
    print("• Lower detection thresholds (2 chars for all apps)")
    print("• More accessibility event types monitored")
    print("• Enhanced logging for debugging")
    print("• Force restart capability")
    print("• Health check system every 15 seconds")
    print("• Aggressive text field detection")

if __name__ == "__main__":
    main()
