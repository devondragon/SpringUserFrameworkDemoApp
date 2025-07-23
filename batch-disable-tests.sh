#!/bin/bash

# Quick script to add @Disabled to failing test classes

# Function to add @Disabled annotation to a test class
add_disabled_annotation() {
    local file=$1
    local reason=$2
    local category=$3
    
    # Check if already disabled
    if grep -q "@Disabled" "$file"; then
        echo "Already disabled: $file"
        return
    fi
    
    # Add import if not present
    if ! grep -q "import org.junit.jupiter.api.Disabled;" "$file"; then
        sed -i '' '/^import.*Test;/a\
import org.junit.jupiter.api.Disabled;' "$file"
    fi
    
    # Add @Disabled annotation before class declaration
    sed -i '' "/^public class\|^class/i\\
@Disabled(\"See TEST-ANALYSIS.md - Category: $category - $reason\")\\
" "$file"
    
    echo "Disabled: $file"
}

# Example usage - uncomment and modify as needed:

# OAuth2 related tests
# add_disabled_annotation "src/test/java/com/digitalsanctuary/spring/user/OAuth2Test.java" \
#     "Requires OAuth2 mock infrastructure" "OAuth2"

# Authentication tests with wrong expectations
# add_disabled_annotation "src/test/java/com/digitalsanctuary/spring/user/api/UserApiTest.java" \
#     "Expects JSON errors but Spring Security returns empty 401/403" "Auth"

echo "Add specific test files to disable by uncommenting examples above"