# Add faulty components to the PACCOR generated JSON componentsFile.
# This will be used to create a bad platform certificate.

import json

print("Adding Faulty components...")

try:
	nicComponent = '00090002'

	with open("/var/hirs/pc_generation/componentsFile", "r") as f:
		data = json.load(f)
		print(data)
		components = data['COMPONENTS']
		for component in components:
			if component['COMPONENTCLASS']['COMPONENTCLASSVALUE'] == nicComponent:
				print("Creating FAULTY component for: " + component['MODEL'])
				component['MODEL'] += "-FAULTY"
				print("New JSON value: " + component['MODEL'])
	with open("/var/hirs/pc_generation/badComponentsFile", 'w') as outfile:
		json.dump(data, outfile)

except ValueError:
    print("Error processing JSON")
