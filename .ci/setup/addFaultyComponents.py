# Add faulty components to the PACCOR generated JSON componentsFile.
# This will be used to create a bad platform certificate.
# Will not need this once PACCOR supports generation of faulty components.

import json

print("Adding Faulty components...")

try:
	nicComponent = '00090002'
	pc_dir = '/var/hirs/pc_generation/'

	with open(pc_dir + "componentsFile", "r") as f:

		data = json.load(f)
		print(data)
		components = data['COMPONENTS']
		for component in components:
			if component['COMPONENTCLASS']['COMPONENTCLASSVALUE'] == nicComponent:
				print("Creating FAULTY component for: " + component['MODEL'])
				component['MODEL'] += "-FAULTY"
				print("New JSON value: " + component['MODEL'])

	with open(pc_dir + "badComponentsFile", 'w') as outfile:
		json.dump(data, outfile)

except Exception:
    print("=== ERROR generating badComponentsFile ===")
