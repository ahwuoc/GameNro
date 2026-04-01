#!/bin/bash
# Script to check code metrics

echo "📊 GameNro Code Metrics Report"
echo "================================"
echo ""

# Check if tokei is installed
if ! command -v tokei &> /dev/null; then
    echo "⚠️  tokei not found. Installing..."
    cargo install tokei
fi

echo "📈 Overall Statistics:"
tokei src/

echo ""
echo "🔍 Large Files (> 300 lines):"
find src -name "*.rs" -exec wc -l {} + | sort -rn | awk '$1 > 300 {print $1 " lines: " $2}' | head -20

echo ""
echo "🎯 Target Metrics:"
echo "  - Files > 500 lines: $(find src -name "*.rs" -exec wc -l {} + | awk '$1 > 500' | wc -l) (target: 0)"
echo "  - Files > 400 lines: $(find src -name "*.rs" -exec wc -l {} + | awk '$1 > 400' | wc -l) (target: 0)"
echo "  - Files > 300 lines: $(find src -name "*.rs" -exec wc -l {} + | awk '$1 > 300' | wc -l) (target: < 10)"

echo ""
echo "✅ Metrics check complete!"
