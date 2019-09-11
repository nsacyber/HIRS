# Add faulty components to the PACCOR generated JSON componentsFile.
# This will be used to create a bad platform certificate.

import json
import pprint

try:
	badComponent = '00030003'
	pcDir = '/var/hirs/pc_generation/'
	paccorComponentsFile = 'componentsFile'
	pBaseJsonFileOut = 'PBaseCertB.componentlist.json'

	# Open the paccor components file
	with open(pcDir + paccorComponentsFile, "r") as f:

		# Load the info from the componentsFile
		data = json.load(f)
		print("The %s info:" % (paccorComponentsFile))
		pp = pprint.PrettyPrinter(indent=4)
		pp.pprint(data)

		# Find the component to use as "FAULTY"
		for component in data['COMPONENTS']:
			if component['COMPONENTCLASS']['COMPONENTCLASSVALUE'] == badComponent:
				print("Creating FAULTY component for: " + component['MODEL'])
				component['MODEL'] += "-FAULTY"
				print("New JSON value: " + component['MODEL'])
				break

	# Write the new JSON file to be used in creating the PBaseCertB certificate.
	with open(pcDir + pBaseJsonFileOut, 'w') as outfile:
		print("Writing %s%s ..." % (pcDir, pBaseJsonFileOut))
		json.dump(data, outfile)
		pp = pprint.PrettyPrinter(indent=4)
		pp.pprint(data)

except Exception as ex:
    print("=== ERROR generating PBaseCertB JSON files: %s" % (ex.message))
