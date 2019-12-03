# Create JSON files needed to create the following certificates:
#    PBaseCertA - Good Base
#    SIDeltaCertA1 - Good Delta
#    SIDeltaCertA2 - Bad Delta
#    SIDeltaCertA2Resolved - Good Delta
#    SIDeltaCertA3 - Good Delta
#    VARDeltaCertA1 - Good Delta
#    VARDeltaCertA2 - Bad Delta
#    VARDeltaCertA2Resolved - Good Delta

import sys
import json
import copy
import pprint

try:
	minNumOfComponents = 3
	maxComponentsToFind = 2
	numComponentsFound = 0
	delComponent1AtIndex = 0
	delComponent2AtIndex = 0
	badComponent = '00030003'
	pcDir = '/var/hirs/pc_generation/'
	paccorComponentsFile = 'componentsFile'
	pBaseJsonFileOut = 'PBaseCertA.componentlist.json'
	siDeltaA1JsonFileOut = 'SIDeltaCertA1.componentlist.json'
	siDeltaA2JsonFileOut = 'SIDeltaCertA2.componentlist.json'
	siDeltaA2ResolvedJsonFileOut = 'SIDeltaCertA2.resolved.componentlist.json'
	siDeltaA3JsonFileOut = 'SIDeltaCertA3.componentlist.json'
	varDeltaA1JsonFileOut = 'VARDeltaCertA1.componentlist.json'
	varDeltaA2JsonFileOut = 'VARDeltaCertA2.componentlist.json'
	varDeltaA2ResolvedJsonFileOut = 'VARDeltaCertA2.resolved.componentlist.json'

	# Open the paccor components file
	with open(pcDir + paccorComponentsFile, "r") as f:

		# Load the info from the componentsFile
		data = json.load(f)
		print("The %s info:" % (paccorComponentsFile))
		pp = pprint.PrettyPrinter(indent=4)
		pp.pprint(data)

		# Initialize the base/delta structures
		pBaseComponentDict = copy.deepcopy(data)
 		siDeltaA1ComponentDict = copy.deepcopy(data)
 		siDeltaA2ComponentDict = copy.deepcopy(data)
 		siDeltaA2ResolvedComponentDict = copy.deepcopy(data)
 		siDeltaA3ComponentDict = copy.deepcopy(data)
 		varDeltaA1ComponentDict = copy.deepcopy(data)
 		numOfComponents = len(data['COMPONENTS'])

		print("Total number of components: %d." % numOfComponents)

		# Need at least three components to run system tests
		if numOfComponents < minNumOfComponents:
			raise Exception("Need at least %d components to run system tests!" % minNumOfComponents)
		else:
			print("Splitting into 1 base and multiple delta JSON files to generate the certs...")

			# Setup good base. Find the first two components that have a Serial included.
			for i in range(len(pBaseComponentDict['COMPONENTS'])):
				print("Current component[%d]:" % i)
				pp.pprint(pBaseComponentDict['COMPONENTS'][i])
				if 'SERIAL' in pBaseComponentDict['COMPONENTS'][i]:
					print("SERIAL found: %s" % pBaseComponentDict['COMPONENTS'][i]['SERIAL'])
					numComponentsFound += 1
				else:
					print("SERIAL not found.")

				tmpComponent = copy.deepcopy(pBaseComponentDict['COMPONENTS'][i])

				# Check if we found 2 components
				if numComponentsFound == 1:
					delComponent1AtIndex = i

					# Use component for the SIDeltaA1
					del siDeltaA1ComponentDict['COMPONENTS'][:]
					siDeltaA1ComponentDict['COMPONENTS'].append(tmpComponent)
					siDeltaA1ComponentDict['COMPONENTS'][0]['STATUS'] = "ADDED"

				elif numComponentsFound == 2:
					delComponent2AtIndex = i

					# Use component for the VARDeltaA1
					del varDeltaA1ComponentDict['COMPONENTS'][:]
					varDeltaA1ComponentDict['COMPONENTS'].append(tmpComponent)
					varDeltaA1ComponentDict['COMPONENTS'][0]['STATUS'] = "ADDED"
					break

		  	# Raise exception if we don't have two components with serial numbers.
			if numComponentsFound < 2:
				raise Exception("Need at least 2 components with SERIAL NUMBERS to run system tests!")
			else:
				print ("We're OK!")

 			# Delete the two components from pBaseComponentDict
 			del pBaseComponentDict['COMPONENTS'][delComponent2AtIndex]
 			del pBaseComponentDict['COMPONENTS'][delComponent1AtIndex]

			# Setup bad and good delta...
			# Create SIDeltaA2 with one component, MODEL as "-FAULTY", STATUS as "MODIFIED"
			# Create SIDeltaA2_resolved with one component, MODEL as "-FAULTY", STATUS as "REMOVED"
			del siDeltaA2ComponentDict['COMPONENTS'][:]
			del siDeltaA2ResolvedComponentDict['COMPONENTS'][:]
			for component in data['COMPONENTS']:
				if component['COMPONENTCLASS']['COMPONENTCLASSVALUE'] == badComponent:
					siDeltaA2Component = copy.copy(component)
					siDeltaA2Component['STATUS'] = "MODIFIED"
					siDeltaA2Component['MODEL'] += "-FAULTY"
					siDeltaA2ComponentDict['COMPONENTS'].append(siDeltaA2Component)

					siDeltaA2ResolvedComponent = copy.copy(siDeltaA2Component)
					siDeltaA2ResolvedComponent['STATUS'] = "REMOVED"
					siDeltaA2ResolvedComponentDict['COMPONENTS'].append(siDeltaA2ResolvedComponent)
					break

			# Setup good delta...
			# Create SIDeltaA3 with component "REMOVED" from SIDeltaA1
			del siDeltaA3ComponentDict['COMPONENTS'][:]
			siDeltaA3ComponentDict['COMPONENTS']= copy.deepcopy(siDeltaA1ComponentDict['COMPONENTS'])
			siDeltaA3ComponentDict['COMPONENTS'][0]['STATUS'] = "REMOVED"

			# Setup bad delta...
			# Create VARDeltaA2 with a component that is not in the Base
			varDeltaA2ComponentDict = copy.deepcopy(varDeltaA1ComponentDict)
			varDeltaA2ComponentDict['COMPONENTS'][0]['MODEL'] = "This component is not in Base"
			varDeltaA2ComponentDict['COMPONENTS'][0]['SERIAL'] = "1234567"
			varDeltaA2ComponentDict['COMPONENTS'][0]['STATUS'] = "ADDED"

			# Setup good delta...
			# Create VARDeltaA2_resolved
			varDeltaA2ResolvedComponentDict = copy.deepcopy(varDeltaA2ComponentDict)
			varDeltaA2ResolvedComponentDict['COMPONENTS'][0]['STATUS'] = "REMOVED"

			# Write the new JSON file to be used in creating the PBaseCertA certificate.
			with open(pcDir + pBaseJsonFileOut, 'w') as outfile:
				print("Writing %s%s ..." % (pcDir, pBaseJsonFileOut))
		 		json.dump(pBaseComponentDict, outfile)
				pp = pprint.PrettyPrinter(indent=4)
				pp.pprint(pBaseComponentDict)

		 	# Write the new JSON file to be used in creating the SIDeltaA1 certificate.
			with open(pcDir + siDeltaA1JsonFileOut, 'w') as outfile:
		 		print("Writing %s%s ..." % (pcDir, siDeltaA1JsonFileOut))
		 		json.dump(siDeltaA1ComponentDict, outfile)
		 		pp = pprint.PrettyPrinter(indent=4)
				pp.pprint(siDeltaA1ComponentDict)

			# Write the new JSON file to be used in creating the SIDeltaA2 certificate.
			with open(pcDir + siDeltaA2JsonFileOut, 'w') as outfile:
		 		print("Writing %s%s ..." % (pcDir, siDeltaA2JsonFileOut))
		 		json.dump(siDeltaA2ComponentDict, outfile)
		 		pp = pprint.PrettyPrinter(indent=4)
				pp.pprint(siDeltaA2ComponentDict)

			# Write the new JSON file to be used in creating the SIDeltaA2Resolved certificate.
			with open(pcDir + siDeltaA2ResolvedJsonFileOut, 'w') as outfile:
		 		print("Writing %s%s ..." % (pcDir, siDeltaA2ResolvedJsonFileOut))
		 		json.dump(siDeltaA2ResolvedComponentDict, outfile)
		 		pp = pprint.PrettyPrinter(indent=4)
				pp.pprint(siDeltaA2ResolvedComponentDict)

			# Write the new JSON file to be used in creating the SIDeltaA3 certificate.
			with open(pcDir + siDeltaA3JsonFileOut, 'w') as outfile:
		 		print("Writing %s%s ..." % (pcDir, siDeltaA3JsonFileOut))
		 		json.dump(siDeltaA3ComponentDict, outfile)
		 		pp = pprint.PrettyPrinter(indent=4)
				pp.pprint(siDeltaA3ComponentDict)

			# Write the new JSON file to be used in creating the VARDeltaA1 certificate.
			with open(pcDir + varDeltaA1JsonFileOut, 'w') as outfile:
		 		print("Writing %s%s ..." % (pcDir, varDeltaA1JsonFileOut))
		 		json.dump(varDeltaA1ComponentDict, outfile)
		 		pp = pprint.PrettyPrinter(indent=4)
				pp.pprint(varDeltaA1ComponentDict)

			# Write the new JSON file to be used in creating the VARDeltaA2 certificate.
			with open(pcDir + varDeltaA2JsonFileOut, 'w') as outfile:
		 		print("Writing %s%s ..." % (pcDir, varDeltaA2JsonFileOut))
		 		json.dump(varDeltaA2ComponentDict, outfile)
		 		pp = pprint.PrettyPrinter(indent=4)
				pp.pprint(varDeltaA2ComponentDict)

			# Write the new JSON file to be used in creating the VARDeltaA2Resolved certificate.
			with open(pcDir + varDeltaA2ResolvedJsonFileOut, 'w') as outfile:
		 		print("Writing %s%s ..." % (pcDir, varDeltaA2ResolvedJsonFileOut))
		 		json.dump(varDeltaA2ResolvedComponentDict, outfile)
		 		pp = pprint.PrettyPrinter(indent=4)
				pp.pprint(varDeltaA2ResolvedComponentDict)

except Exception as ex:
	print("=== ERROR generating PBaseCertA JSON files: %s" % (ex.message))
