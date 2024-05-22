package xrd.zerocollateral


import kotlinx.serialization.json.*
import java.io.File

fun main() {
    // Define the directory path where your JSON files are located
    val dirPath = "feedback/tutti"
    println("Processing directory: ${dirPath}")

    // Initialize CSV file
//    extracted(dirPath)
    //output file where manifest will be written
    val csvFile = File("loanprotection.txt")
    csvFile.bufferedWriter().use { out ->
        // Initialize set to track processed addresses
        val processedAddresses = HashSet<String>()

        // Initialize counts map
        val totalProtectionCounts = mutableMapOf<String, Int>()

        // Iterate over files in the directory
        File(dirPath).walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "json") {
                println("Processing file: ${file.name}")
                // Read and parse JSON
                val jsonString = file.readText()
                val json = Json.parseToJsonElement(jsonString)

                // Extract loan protection information
                val loanProtection = json.jsonObject["loan_protection"]

                // Count occurrences of each loan protection method
                val protectionCounts = when (loanProtection) {
                    is JsonPrimitive -> mapOf(loanProtection.content.trim() to 1)
                    is JsonArray -> loanProtection.mapNotNull { (it as? JsonPrimitive)?.content?.trim() }
                        .groupBy { it }
                        .mapValues { it.value.size }
                    else -> null
                }

                // Update totalProtectionCounts with protectionCounts
                if (protectionCounts != null) {
                    protectionCounts.forEach { (method, count) ->
                        totalProtectionCounts[method] = (totalProtectionCounts[method] ?: 0) + count
                    }
                }
            }
        }

        // Print totalProtectionCounts
        println("Total Protection Counts:")
        totalProtectionCounts.forEach { (method, count) ->
            println("$method: $count")
            out.write("$method: $count")
            out.write("\n")
        }

    }
}

private fun extracted(dirPath: String) {
    val csvFile = File("output.csv")
    csvFile.bufferedWriter().use { out ->
        // Initialize set to track processed addresses
        val processedAddresses = HashSet<String>()

        // Iterate over files in the directory
        File(dirPath).walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "json") {
                println("Processing file: ${file.name}")
                // Read and parse JSON
                val jsonString = file.readText()
                val json = Json.parseToJsonElement(jsonString)

                // Extract address and amount
                val address = json.jsonObject["stokenet_address"]?.jsonPrimitive?.contentOrNull
                val amount = 10
                /*                    json.jsonObject["amount"]?.jsonPrimitive?.intOrNull*/
                println("address found: ${address}")

                // Write to CSV if address is not null and not already processed
                if (address != null && amount != null && processedAddresses.add(address)) {
                    out.write("$address,$amount\n")
                    println("Data written to CSV for file: ${file.name}")
                }
            }
        }
    }
}

/*
CALL_METHOD
Address("account_rdx12x7yygrp6k2lppptarwtdfpcyckynsryx9r67kqlmtzz5e2hc0kac2")
"lock_fee"
Decimal("0.6079098788425")
;
CALL_METHOD
Address("account_rdx12x7yygrp6k2lppptarwtdfpcyckynsryx9r67kqlmtzz5e2hc0kac2")
"withdraw"
Address("resource_rdx1t5vx7fx3xsp6npxhhgefqgk3qdjdhalntwskkspa4yper0tltzjlw8")
Decimal("10")
;
TAKE_FROM_WORKTOP
Address("account_rdx12x7yygrp6k2lppptarwtdfpcyckynsryx9r67kqlmtzz5e2hc0kac2")
Decimal("10")
Bucket("bucket1")
;
CALL_METHOD
Address("mainet_address")
"try_deposit_or_abort"
Bucket("bucket1")
Enum<0u8>()
;
*/


