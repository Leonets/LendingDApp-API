cd ..
./gradlew shadowJar
cd deploy
terraform apply -var-file terraform.tfvars